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
  public List<SubmissionResponse> getHistoryForTeamAndLevel(UUID teamId, Long levelId) {
    return submissionRepo.findLatestByTeamAndLevel(teamId, levelId).stream()
        .map(s -> toResponse(s, false))
        .collect(Collectors.toList());
  }

  /**
   * All the details for 1 submissions, this includes a teams complete scoring log from the blob
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
   * Gets the scoring log for a team according to level from the blob storage. This is all the
   * submissions that the team has made to a specific level.
   */
  @Transactional(readOnly = true)
  public ScoringLogResponse getTeamScoringLog(UUID teamId, UUID eventId, Long levelId) {
    Optional<ScoringLog> metaData =
        scoringLogRepo.findByTeamIdAndEventIdAndLevelId(teamId, eventId, levelId);
    if (metaData.isEmpty()) {
      return null;
    }
    return toLogResponse(metaData.get());
  }

  /** Returns only the meta data for all levels a team has submitted to in a specific event. */
  @Transactional(readOnly = true)
  public List<ScoringLogResponse> getAllLevelLogsForTeam(UUID teamId, UUID eventId) {
    return scoringLogRepo.findByTeamIdAndEventId(teamId, eventId).stream()
        .map(this::toLogResponseMetDataOnly)
        .collect(Collectors.toList());
  }

  private SubmissionResponse toResponse(Submission sub, boolean incLog) {
    ScoringLogResponse log = null;
    if (incLog && sub.getEventId() != null) {
      Optional<ScoringLog> metaData =
          scoringLogRepo.findByTeamIdAndEventIdAndLevelId(
              sub.getTeamId(), sub.getEventId(), sub.getLevelId());
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
        metaData.getTeamId(),
        metaData.getEventId(),
        metaData.getStorageKey(),
        metaData.getSubmissionCount(),
        metaData.getLastUpdatedAt(),
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

  private ScoringLogResponse toLogResponseMetDataOnly(ScoringLog MD) {
    return new ScoringLogResponse(
        MD.getTeamId(),
        MD.getEventId(),
        MD.getStorageKey(),
        MD.getSubmissionCount(),
        MD.getLastUpdatedAt(),
        null);
  }
}