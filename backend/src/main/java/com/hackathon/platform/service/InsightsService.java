package com.hackathon.platform.service;

import com.hackathon.platform.dto.AdminDashboardResponse;
import com.hackathon.platform.dto.EventInsightsResponse;
import com.hackathon.platform.dto.LevelScoreStats;
import com.hackathon.platform.dto.SubmissionRateBucket;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Computes admin facing statistics */
@Service
public class InsightsService {

  private static final List<String> SUBMISSION_STATUSES =
      List.of("QUEUED", "SCORING", "SCORED", "FAILED");
  private final EventRepository eventRepository;
  private final SubmissionRepository submissionRepository;
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;

  public InsightsService(
      EventRepository eventRepository,
      SubmissionRepository submissionRepository,
      TeamRepository teamRepository,
      TeamMemberRepository teamMemberRepository) {
    this.eventRepository = eventRepository;
    this.submissionRepository = submissionRepository;
    this.teamRepository = teamRepository;
    this.teamMemberRepository = teamMemberRepository;
  }

  /** Overview across all events by appropriate admin */
  public AdminDashboardResponse getAdminDashboard(UUID adminUserId) {
    List<Event> events = eventRepository.fetchAllByAdmin(adminUserId);

    long activeEvents = events.stream().filter(e -> isActiveStatus(e.getStatus())).count();

    long totalParticipants =
        events.stream().mapToLong(e -> countApprovedParticipants(e.getEventId())).sum();

    Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
    long submissionsToday = submissionRepository.countByAdminSince(adminUserId, startOfToday);
    long totalSubmissions = submissionRepository.countByAdmin(adminUserId);

    return new AdminDashboardResponse(
        activeEvents, events.size(), totalParticipants, submissionsToday, totalSubmissions);
  }

  /** Dashboard stats for a single event */
  public EventInsightsResponse getEventInsights(UUID eventId, int trendWindowMinutes) {

    long activeTeams = teamRepository.countByEventIdAndStatus(eventId, "ACTIVE");
    long approvedParticipants = countApprovedParticipants(eventId);

    long totalSubmissions = submissionRepository.countByEventId(eventId);

    Instant hourAgo = Instant.now().minus(Duration.ofHours(1));
    long submissionsLastHour =
        submissionRepository.countByEventIdAndSubmittedAtAfter(eventId, hourAgo);

    Map<String, Long> submissionsByStatus = new HashMap<>();
    SUBMISSION_STATUSES.forEach(status -> submissionsByStatus.put(status, 0L));
    submissionRepository
        .countByEventIdGroupByStatus(eventId)
        .forEach(row -> submissionsByStatus.put(row.getStatus(), row.getTotal()));

    long scored = submissionsByStatus.getOrDefault("SCORED", 0L);
    long failed = submissionsByStatus.getOrDefault("FAILED", 0L);
    long finished = scored + failed;
    Double errorRate = finished == 0 ? null : (double) failed / finished;

    Instant windowStart = Instant.now().minus(Duration.ofMinutes(Math.max(1, trendWindowMinutes)));
    List<SubmissionRateBucket> submissionRate =
        submissionRepository.findSubmissionRateSince(eventId, windowStart).stream()
            .map(row -> new SubmissionRateBucket(row.getBucketStart(), row.getCount()))
            .collect(Collectors.toList());

    List<LevelScoreStats> scoreDistribution =
        submissionRepository.findScoreDistributionByEventId(eventId).stream()
            .map(
                row ->
                    new LevelScoreStats(
                        row.getLevelId(),
                        row.getLevelName(),
                        row.getScoredSubmissions(),
                        row.getMinScore(),
                        row.getMaxScore(),
                        row.getAvgScore()))
            .collect(Collectors.toList());

    return new EventInsightsResponse(
        eventId,
        activeTeams,
        approvedParticipants,
        totalSubmissions,
        submissionsLastHour,
        submissionsByStatus,
        errorRate,
        submissionRate,
        scoreDistribution);
  }

  private long countApprovedParticipants(UUID eventId) {
    List<UUID> teamIds =
        teamRepository.findByEventId(eventId).stream()
            .map(Team::getTeamId)
            .collect(Collectors.toList());

    if (teamIds.isEmpty()) {
      return 0;
    }

    return teamMemberRepository.countByTeamIdInAndStatus(teamIds, "APPROVED");
  }

  private boolean isActiveStatus(String status) {
    if (status == null) {
      return false;
    }
    String upper = status.toUpperCase();
    return upper.equals("ACTIVE") || upper.equals("ONGOING");
  }
}
