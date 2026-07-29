package com.hackathon.platform.scoring;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.dto.ScoringLogResponse;
import com.hackathon.platform.dto.SubmissionResponse;
import com.hackathon.platform.model.ScoringLog;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.ScoringLogRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.service.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Queries for submissions */
@Service
@RequiredArgsConstructor
public class SubmissionQueryService {
  private static final Logger logger = LoggerFactory.getLogger(SubmissionQueryService.class);

  private final SubmissionRepository submissionRepo;
  private final ScoringLogRepository scoringLogRepo;
  private final TeamRepository teamRepo;
  private final EventRepository eventRepo;
  private final StorageService storageService;
  private final AzureBlobConfig blobConfig;

  /** All the submissions for a team across all of the levels, ordered by most recent first. */
  @Transactional(readOnly = true)
  public List<SubmissionResponse> getHistoryForTeam(UUID teamId) {
    return submissionRepo.findByTeamId(teamId).stream()
        .sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()))
        .map(s -> toResponse(s, false))
        .collect(Collectors.toList());
  }

  /** Submissions for a team based on a specific level, ordered by most recent first. */
  @Transactional(readOnly = true)
  public List<SubmissionResponse> getHistoryForTeamAndLevel(UUID teamId, short levelId) {
    return submissionRepo.findLatestByTeamAndLevel(teamId, levelId).stream()
        .map(s -> toResponse(s, false))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<SubmissionResponse> getRecentSubmissions(UUID userId, int limit) {
    return submissionRepo.getRecentSubmissions(userId, PageRequest.of(0, limit)).stream()
        .map(s -> toResponse(s, false))
        .collect(Collectors.toList());
  }

  /**
   * All the details for 1 submissions, this includes this submission's own scoring log from blob
   * storage. A team can only view their own feedback.
   */
  @Transactional(readOnly = true)
  public SubmissionResponse getSubmissionDetail(Long subId, UUID teamId) {
    Submission sub =
        submissionRepo
            .findByIdAndTeamId(subId, teamId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "The submission could not be for this team: " + subId));
    return toResponse(sub, true);
  }

  /**
   * Same as the previous, only that this can get details for any submissions regardless of the
   * teamId.
   */
  @Transactional(readOnly = true)
  public SubmissionResponse getSubmissionDetailForAdmin(Long subId) {
    Submission sub =
        submissionRepo
            .findById(subId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "The following submission could not be found: " + subId));
    return toResponse(sub, true);
  }

  /**
   * Gets the scoring log for exactly one submission, scoped to the owning team so a participant
   * can't view another team's feedback by guessing IDs.
   */
  @Transactional(readOnly = true)
  public ScoringLogResponse getScoringLogForSubmission(Long submissionId, UUID teamId) {
    Submission sub =
        submissionRepo
            .findByIdAndTeamId(submissionId, teamId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "The submission could not be for this team: " + submissionId));
    return scoringLogRepo.findBySubmissionId(sub.getId()).map(this::toLogResponse).orElse(null);
  }

  /** Admin variant: the scoring log for any submission regardless of team. */
  @Transactional(readOnly = true)
  public ScoringLogResponse getScoringLogForSubmissionAsAdmin(Long submissionId) {
    return scoringLogRepo.findBySubmissionId(submissionId).map(this::toLogResponse).orElse(null);
  }

  /**
   * Every log a team has for a level in an event, one per submission, most recent submission first.
   */
  @Transactional(readOnly = true)
  public List<ScoringLogResponse> getLevelLogsForTeam(UUID teamId, UUID eventId, Long levelId) {
    return scoringLogRepo
        .findByTeamIdAndEventIdAndLevelIdOrderByCreatedAtDesc(teamId, eventId, levelId)
        .stream()
        .map(this::toLogResponseMetaDataOnly)
        .collect(Collectors.toList());
  }

  /** Returns only the meta data for every scoring log a team has across an event. */
  @Transactional(readOnly = true)
  public List<ScoringLogResponse> getAllLevelLogsForTeam(UUID teamId, UUID eventId) {
    return scoringLogRepo.findByTeamIdAndEventId(teamId, eventId).stream()
        .map(this::toLogResponseMetaDataOnly)
        .collect(Collectors.toList());
  }

  private SubmissionResponse toResponse(Submission sub, boolean incLog) {
    ScoringLogResponse log = null;
    if (incLog) {
      Optional<ScoringLog> metaData = scoringLogRepo.findBySubmissionId(sub.getId());
      if (metaData.isPresent()) {
        log = toLogResponse(metaData.get());
      }
    }

    return new SubmissionResponse(
        sub.getId(),
        sub.getTeamId(),
        sub.getLevelId(),
        sub.getSolverVersionId(),
        sub.getScore(),
        sub.getStatus(),
        sub.getSubmittedAt(),
        sub.getOutputFileName(),
        sub.getSourceFileName(),
        log);
  }

  private ScoringLogResponse toLogResponse(ScoringLog metaData) {
    String content = downloadLogContent(metaData.getStorageKey());
    return new ScoringLogResponse(
        metaData.getSubmissionId(),
        metaData.getTeamId(),
        metaData.getEventId(),
        metaData.getStorageKey(),
        metaData.getCreatedAt(),
        content);
  }

  private String downloadLogContent(String storageKey) {
    try (InputStream in =
        storageService.download(blobConfig.getScoringLogsContainer(), storageKey)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.warn("the scoring Log {} could not be downloaded: {}", storageKey, e.getMessage());
      return "[Content unavailable]";
    }
  }

  private ScoringLogResponse toLogResponseMetaDataOnly(ScoringLog metaData) {
    return new ScoringLogResponse(
        metaData.getSubmissionId(),
        metaData.getTeamId(),
        metaData.getEventId(),
        metaData.getStorageKey(),
        metaData.getCreatedAt(),
        null);
  }
}
