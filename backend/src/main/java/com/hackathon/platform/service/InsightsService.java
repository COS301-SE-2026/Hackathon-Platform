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

/**Computes admin facing statistics */
@Service
public class InsightsService {

    private static final List<String> SUBMISSION_STATUSES = List.of("QUEUED", "SCORING", "SCORED", "FAILED");
    private final EventRepository eventRepository;
    private final SubmissionRepository submissionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public InsightsService(
        EventRepository eventRepository,
        SubmissionRepository submissionRepository,
        TeamRepository teamRepository,
        TeamMemberRepository teamMemberRepository
    ) {
        this.eventRepository = eventRepository;
        this.submissionRepository = submissionRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    /** Overview across all events by appropriate admin */
    public AdminDashboardResponse getAdminDashboard(UUID adminUserId) {
        List<Event> events = eventRepository.fetchAllByAdmin(adminUserId);

        long activeEvents =
            events.stream().filter(e -> isActiveStatus(e.getStatus())).count();

        long totalParticipants =
            events.stream()
                .mapToLong(e -> countApprovedParticipants(e.getEventId()))
                .sum();

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        long submissionsToday = submissionRepository.countByAdminSince(adminUserId, startOfToday);

        return new AdminDashboardResponse(
            activeEvents, events.size(), totalParticipants, submissionsToday);
    }


}
