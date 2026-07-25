package com.hackathon.platform.service;

import com.hackathon.platform.model.Hackathon;
import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.model.SolverVersion;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.HackathonRepository;
import com.hackathon.platform.repository.LevelFileRepository;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.storage.BlobPath;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileMetadataService {

  private final LevelFileRepository levelFileRepository;
  private final SolverVersionRepository solverVersionRepository;
  private final SubmissionRepository submissionRepository;
  private final HackathonRepository hackathonRepository;

  @Transactional
  public LevelFile saveLevelFile(
      short levelId,
      String fileName,
      String storageKey,
      String fileType,
      Long fileSize,
      String contentType) {
    LevelFile levelFile = new LevelFile((long)levelId, fileName, storageKey, fileType);
    levelFile.setFileSize(fileSize);
    levelFile.setContentType(contentType);
    levelFile.setUpdatedAt(Instant.now());

    LevelFile saved = levelFileRepository.save(levelFile);
    log.info("Saved level file metadata: levelId={}, storageKey={}", levelId, storageKey);
    return saved;
  }

  @Transactional
  public SolverVersion saveSolverVersion(
      UUID hackathonId,
      UUID uploadedBy,
      String storageKey,
      Integer versionNumber,
      String fileName,
      Long fileSize,
      String notes) {
    SolverVersion solverVersion = new SolverVersion(hackathonId, uploadedBy, storageKey);
    solverVersion.setVersionNumber(versionNumber);
    solverVersion.setFileName(fileName);
    solverVersion.setFileSize(fileSize);
    solverVersion.setIsActive(true);
    solverVersion.setUploadedAt(Instant.now());
    solverVersion.setNotes(notes);

    SolverVersion saved = solverVersionRepository.save(solverVersion);
    log.info(
        "Saved solver version metadata: hackathonId={}, version={}, storageKey={}",
        hackathonId,
        versionNumber,
        storageKey);
    return saved;
  }

  @Transactional
  public Submission saveSubmission(
      String eventId,
      UUID teamId,
      short levelId,
      Long solverVersionId,
      String outputFileName,
      Long outputFileSize,
      String outputContentType,
      String sourceFileName,
      Long sourceFileSize,
      String sourceContentType) {

    // Initial save with placeholders just to get a DB-generated id
    Submission submission = new Submission(teamId, levelId, solverVersionId, "pending", "pending");
    submission.setEventId(UUID.fromString(eventId));
    submission.setOutputFileName(outputFileName);
    submission.setOutputFileSize(outputFileSize);
    submission.setOutputContentType(outputContentType);
    submission.setSourceFileName(sourceFileName);
    submission.setSourceFileSize(sourceFileSize);
    submission.setSourceContentType(sourceContentType);
    submission.setStatus("QUEUED");
    submission.setSubmittedAt(Instant.now());

    Submission saved = submissionRepository.save(submission);

    // Build canonical keys using the real DB id
    String dbId = String.valueOf(saved.getId());
    String levelIdStr = String.valueOf(levelId);
    String outputKey =
        BlobPath.submissionOutput(eventId, teamId.toString(), levelIdStr, dbId, outputFileName);
    String sourceKey =
        BlobPath.submissionSourceArchive(
            eventId, teamId.toString(), levelIdStr, dbId, sourceFileName);

    saved.setOutputStorageKey(outputKey);
    saved.setSourceCodeStorageKey(sourceKey);
    saved = submissionRepository.save(saved);

    log.info(
        "Created submission record: id={}, outputKey={}, sourceKey={}",
        saved.getId(),
        outputKey,
        sourceKey);
    return saved;
  }

  @Transactional
  public Hackathon updateProblemStatementStorageKey(UUID hackathonId, String storageKey) {
    Hackathon hackathon =
        hackathonRepository
            .findById(hackathonId)
            .orElseThrow(() -> new RuntimeException("Hackathon not found: " + hackathonId));
    hackathon.setProblemStatementStorageKey(storageKey);
    Hackathon saved = hackathonRepository.save(hackathon);
    log.info("Updated problem statement storage key: hackathonId={}", hackathonId);
    return saved;
  }

  @Transactional(readOnly = true)
  public List<LevelFile> listLevelFiles(Long levelId) {
    return levelFileRepository.findByLevelId(levelId);
  }

  @Transactional(readOnly = true)
  public LevelFile getLevelFile(Long fileId) {
    return levelFileRepository
        .findById(fileId)
        .orElseThrow(() -> new RuntimeException("Level file not found: " + fileId));
  }

  @Transactional
  public void deleteLevelFile(Long fileId) {
    if (!levelFileRepository.existsById(fileId)) {
      throw new RuntimeException("Level file not found: " + fileId);
    }
    levelFileRepository.deleteById(fileId);
    log.info("Deleted level file metadata: fileId={}", fileId);
  }

  @Transactional(readOnly = true)
  public String getLevelFileStorageKey(Long levelId, String fileName) {
    return levelFileRepository
        .findByLevelIdAndFileName(levelId, fileName)
        .map(LevelFile::getStorageKey)
        .orElseThrow(
            () ->
                new RuntimeException(
                    String.format(
                        "Level file not found: levelId=%d, fileName=%s", levelId, fileName)));
  }

  @Transactional(readOnly = true)
  public String getSubmissionOutputStorageKey(Long submissionId) {
    return submissionRepository
        .findById(submissionId)
        .map(Submission::getOutputStorageKey)
        .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));
  }

  @Transactional(readOnly = true)
  public String getSubmissionSourceStorageKey(Long submissionId) {
    return submissionRepository
        .findById(submissionId)
        .map(Submission::getSourceCodeStorageKey)
        .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));
  }
}
