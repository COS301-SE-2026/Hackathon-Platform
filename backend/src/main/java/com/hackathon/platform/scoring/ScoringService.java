package com.hackathon.platform.scoring;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.model.ScoringLog;
import com.hackathon.platform.model.SolverVersion;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.repository.LevelFileRepository;
import com.hackathon.platform.repository.ScoringLogRepository;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.service.StorageService;
import com.hackathon.platform.storage.BlobPath;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scores a single submission in the following order:
 *
 * <p>1. Looks for the submission, solver version, levels input files. 2. Downloads the solver
 * script, participants output file, level input from blob storage. 3. calls the SolverRunner with a
 * hard timeout. 4. adds score and status to the submission. 5. adds the scoring log to the team's
 * log file in blob storage and updates the meta data in the scoringlogs table.
 */
@Service
@RequiredArgsConstructor
public class ScoringService {
  private static final Logger logger = LoggerFactory.getLogger(ScoringService.class);
  private static final DateTimeFormatter loggerTimeStamp =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

  private final SubmissionRepository submissionRepo;
  private final SolverVersionRepository solverVRepo;
  private final LevelFileRepository levelFRepo;
  private final ScoringLogRepository scoringLogRepo;
  private final TeamRepository teamRepo;
  private final StorageService storageService;
  private final AzureBlobConfig blobConfig;
  private final SolverRunner solverRunner;

  /**
   * Scores the submission, can be recalled, each call appends a new log to the teams log file.
   *
   * @param submissionId the submission we are scoring
   * @return new submission with score and status set
   */

  public Submission scoreSubmission(Long submissionId) {
    Submission sub = markAsScoring(submissionId);

    Team team =
        teamRepo
            .findById(sub.getTeamId())
            .orElseThrow(() -> new IllegalArgumentException("Team could not be found "));
    SolverVersion solverVersion =
        solverVRepo
            .findById(sub.getSolverVersionId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No solver version could be found for the submission"));

    UUID eventId = sub.getEventId();
    UUID teamId = team.getTeamId();

    Path tempDir = null;
    String logString;
    try {
      tempDir = Files.createTempDirectory("scoring-fetch-");
      Path solverScript =
          downloadToLocal(
              blobConfig.getEventResourcesContainer(),
              solverVersion.getStorageKey(),
              tempDir,
              "solver.py");
      Path outputFile =
          downloadToLocal(
              blobConfig.getSubmissionsContainer(),
              sub.getOutputStorageKey(),
              tempDir,
              sub.getOutputFileName() != null ? sub.getOutputFileName() : "output");
      Path levelInputDir = downloadLvlInputs(sub.getLevelId(), tempDir);
      SolverRunOutcome outcome = solverRunner.run(solverScript, outputFile, levelInputDir);
      applySuccessResult(sub, outcome);
      logString = buildLogString(submissionId, eventId, teamId, sub, outcome);
    } catch (SolverExecutionException e) {
      applyFailedResult(sub, e);
      logString = buildFailedStringBlock(submissionId, eventId, teamId, sub, e);
    } catch (IOException e) {
      logger.error(
          "I/O error occured while preparing scoring run for submission {}", submissionId, e);
      SolverExecutionException wrapped =
          new SolverExecutionException(
              "Failed to prepare the files for scoring: " + e.getMessage(), "SOLVER_CRASH");
      applyFailedResult(sub, wrapped);
      logString = buildFailedStringBlock(submissionId, eventId, teamId, sub, wrapped);
    } finally {
      if (tempDir != null) {
        deleteQuietly(tempDir);
      }
    }

    saveResult(sub);
    appendToScoringLog(teamId, eventId, sub.getLevelId(), logString);
    return sub;
  }

  @Transactional
  public Submission markAsScoring(Long submissionId){
    Submission sub = submissionRepo.findById(submissionId).orElseThrow(() -> new IllegalArgumentException("Submission wasnt found "+submissionId));
    sub.setStatus("SCORING");
    return submissionRepo.save(sub);
  }

  @Transactional
  public void saveResult(Submission sub){
    submissionRepo.save(sub);
  }

  @Transactional
  public void appendToScoringLog(UUID teamId, UUID eventId, Long levelId, String logString) {
    String storageKey =
        BlobPath.scoringLog(eventId.toString(), teamId.toString(), levelId.toString());
    String content = "";
    if (storageService.exists(blobConfig.getScoringLogsContainer(), storageKey)) {
      try (InputStream in =
          storageService.download(blobConfig.getScoringLogsContainer(), storageKey)) {
        content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException e) {
        logger.warn(
            "The existing scoring log {} could not be read, starting fresh: {}",
            storageKey,
            e.getMessage());
      }
    }

    String newContent = content + logString;
    byte[] contentBytes = newContent.getBytes(StandardCharsets.UTF_8);
    storageService.uploadBytes(
        blobConfig.getScoringLogsContainer(), storageKey, contentBytes, "text/plain");

    Optional<ScoringLog> old =
        scoringLogRepo.findByTeamIdAndEventIdAndLevelId(teamId, eventId, levelId);
    ScoringLog metaData =
        old.orElseGet(() -> new ScoringLog(teamId, eventId, levelId, storageKey));
    metaData.setSubmissionCount(metaData.getSubmissionCount() + 1);
    metaData.setLastUpdatedAt(Instant.now());
    scoringLogRepo.save(metaData);
  }

  private Path downloadToLocal(
      String container, String storageKey, Path tempDir, String localFileName) throws IOException {
    Path target = tempDir.resolve(cleanLocalName(localFileName));
    try (InputStream in = storageService.download(container, storageKey)) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return target;
  }

  private Path downloadLvlInputs(Long levelId, Path tempDir) throws IOException {
    List<LevelFile> files = levelFRepo.findByLevelId(levelId);
    if (files.isEmpty()) {
      return null;
    }
    Path inputDir = tempDir.resolve("level-inputs");
    Files.createDirectories(inputDir);
    for (LevelFile file : files) {
      Path target = inputDir.resolve(cleanLocalName(file.getFileName()));
      try (InputStream in =
          storageService.download(blobConfig.getEventResourcesContainer(), file.getStorageKey())) {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      }
    }

    return inputDir;
  }

  private String cleanLocalName(String name) {
    return name == null ? "file" : name.replaceAll("[/\\\\]", "_");
  }

  private void applySuccessResult(Submission sub, SolverRunOutcome outcome) {
    SolverResult res = outcome.getResult();
    sub.setScore(res.getScore() != null ? res.getScore() : BigDecimal.ZERO);
    sub.setStatus(res.getStatus());
  }

  private void applyFailedResult(Submission sub, SolverExecutionException e) {
    sub.setScore(BigDecimal.ZERO);
    sub.setStatus("FAILED");
  }

  private String buildLogString(
      Long submissionId, UUID eventId, UUID teamId, Submission sub, SolverRunOutcome outcome) {
    SolverResult res = outcome.getResult();
    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            "Submission #%d  |   %s\n", submissionId, loggerTimeStamp.format(Instant.now())));
    sb.append(String.format("Event:     %s\n", eventId));
    sb.append(String.format("Team:      %s\n", teamId));
    sb.append(String.format("Level:     %s\n", sub.getLevelId()));
    sb.append(String.format("Status:    %s\n", res.getStatus()));
    sb.append(String.format("Score:     %s\n", res.getScore()));

    List<String> messages = res.getMessages();
    if (messages != null && !messages.isEmpty()) {
      messages.forEach(m -> sb.append("   ").append(m).append("\n"));
    }

    if (!outcome.getStderr().isBlank()) {
      sb.append("stderr \n");
      sb.append(truncate(outcome.getStderr(), 2000)).append("\n");
    }

    sb.append("\n");
    return sb.toString();
  }

  private String buildFailedStringBlock(
      Long submissionId,
      UUID eventId,
      UUID teamId,
      Submission sub,
      SolverExecutionException e) {
    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            "Submission #%d  |   %s\n", submissionId, loggerTimeStamp.format(Instant.now())));
    sb.append(String.format("Event:     %s\n", eventId));
    sb.append(String.format("Team:      %s\n", teamId));
    sb.append(String.format("Level:     %s\n", sub.getLevelId()));
    sb.append(String.format("Status:    FAILED\n"));
    sb.append(String.format("Score:     0\n"));
    sb.append("\n");
    sb.append(String.format("   Error [%s]: %s\n", e.getErrorType(), e.getMessage()));
    sb.append("\n");

    return sb.toString();
  }

  private String truncate(String text, int maxLen) {
    return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
  }

  private void deleteQuietly(Path dir) {
    try (var stream = Files.walk(dir)) {
      stream
          .sorted((a, b) -> b.getNameCount() - a.getNameCount())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (IOException ignore) {
                }
              });
    } catch (IOException e) {
      logger.warn("Failed to clean up scoring fetch dir {}: {}", dir, e.getMessage());
    }
  }
}