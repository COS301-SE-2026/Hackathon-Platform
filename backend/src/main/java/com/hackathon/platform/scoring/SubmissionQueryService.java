package com.hackathon.platform.scoring;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.dto.ScoringLogResponse;
import com.hackathon.platform.dto.SubmissionResponse;
import com.hackathon.platform.model.ScoringLog;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.repository.ScoringLogRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.service.StorageService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class SubmissionQueryService {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionQueryService.class);

    private final SubmissionRepository submissionRepo;
    private final ScoringLogRepository scoringLogRepo;
    private final TeamRepository teamRepo;
    private final StorageService storageService;
    private final AzureBlobConfig blobConfig;

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getHistoryForTeam(UUID teamId) {
        return submissionRepo.findByTeamId(teamId).stream().sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt())).map(s -> toResponse(s, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getHistoryForTeamAndLevel(UUID teamId, Long levelId) {
        return submissionRepo.findLatestByTeamAndLevel(teamId, levelId).stream().map(s -> toResponse(s, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionDetail(Long subId, UUID teamId) {
        Submission sub = submissionRepo.findByIdAndTeamId(subId, teamId).orElseThrow(() -> new IllegalArgumentException(
            "The submission could not be for this team: " + subId
        ));
        return toResponse(sub, true);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionDetailForAdmin(Long subId) {
        return null;
    }

    private SubmissionResponse toResponse(Submission sub, boolean incLog) {
        ScoringLogResponse log = null;
        if (incLog) {
            Team team = teamRepo.findById(sub.getTeamId()).orElse(null);
            if (team != null) {
                Optional<ScoringLog> metaData = scoringLogRepo.findByTeamIdAndEventIdAndLevelId(sub.getTeamId(), team.getEventId(), sub.getLevelId());
                if (metaData.isPresent()) {
                    log = toLogResponse(metaData.get());
                }
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
            log
        );
    }

    private ScoringLogResponse toLogResponse(ScoringLog metaData) {
        String content = downloadLogContent(metaData.getStorageKey());
        return new ScoringLogResponse(
            metaData.getTeamId(),
            metaData.getEventId(),
            metaData.getStorageKey(),
            metaData.getSubmissionCount(),
            metaData.getLastUpdatedAt(),
            content
        );
    }

    private String downloadLogContent(String storageKey) {
        try (InputStream in = storageService.download(blobConfig.getScoringLogsContainer(), storageKey)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("the scoring Log {} could not be downloaded: {}", storageKey, e.getMessage());
            return "[Content unavailable]";
        }
    }
}