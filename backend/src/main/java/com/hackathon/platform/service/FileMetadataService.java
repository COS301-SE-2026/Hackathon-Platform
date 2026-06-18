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

    
}
