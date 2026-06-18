package com.hackathon.platform.service;

import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.model.SolverVersion;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.LevelFileRepository;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileMetadataService {

    private final LevelFileRepository levelFileRepository;
    private final SolverVersionRepository solverVersionRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional
    public LevelFile saveLevelFile(Long levelId, String fileName, String storageKey,
                                   String fileType, Long fileSize, String contentType) {
        LevelFile levelFile = new LevelFile(levelId, fileName, storageKey, fileType);
        levelFile.setFileSize(fileSize);
        levelFile.setContentType(contentType);
        levelFile.setUpdatedAt(Instant.now());

        LevelFile saved = levelFileRepository.save(levelFile);
        log.info("Saved level file metadata: levelId={}, storageKey={}", levelId, storageKey);
        return saved;
    }

    @Transactional
    public SolverVersion saveSolverVersion(UUID eventId, UUID uploadedBy, String storageKey,
                                           Integer versionNumber, String fileName, Long fileSize) {
        SolverVersion solverVersion = new SolverVersion(eventId, uploadedBy, storageKey);
        solverVersion.setVersionNumber(versionNumber);
        solverVersion.setFileName(fileName);
        solverVersion.setFileSize(fileSize);
        solverVersion.setIsActive(true);
        solverVersion.setUploadedAt(Instant.now());

        SolverVersion saved = solverVersionRepository.save(solverVersion);
        log.info("Saved solver version metadata: eventId={}, version={}, storageKey={}",
                eventId, versionNumber, storageKey);
        return saved;
    }

    @Transactional
    public Submission saveSubmissionOutput(UUID teamId, Long levelId, Long solverVersionId,
                                           String outputStorageKey, String outputFileName,
                                           Long outputFileSize, String outputContentType) {
        Submission submission = new Submission(
                teamId, levelId, solverVersionId,
                "pending", // source key placeholder - updated when source is uploaded
                outputStorageKey);
        submission.setOutputFileName(outputFileName);
        submission.setOutputFileSize(outputFileSize);
        submission.setOutputContentType(outputContentType);
        submission.setStatus("QUEUED");
        submission.setSubmittedAt(Instant.now());

        Submission saved = submissionRepository.save(submission);
        log.info("Created submission record: id={}, outputStorageKey={}", saved.getId(), outputStorageKey);
        return saved;

        
    }
}
