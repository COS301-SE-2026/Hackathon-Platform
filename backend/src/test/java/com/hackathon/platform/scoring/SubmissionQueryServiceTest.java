package com.hackathon.platform.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.SubmissionResponse;
import com.hackathon.platform.model.ScoringLog;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.ScoringLogRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.service.StorageService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmissionQueryServiceTest {
    @Mock private SubmissionRepository subRepo;
    @Mock private ScoringLogRepository scoringLogRepo;
    @Mock private TeamRepository teamRepo;
    @Mock private EventRepository eventRepo;
    @Mock private StorageService storageS;
    @Mock private AzureBlobConfig azureBlob;

    private SubmissionQueryService subQueryService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final Long LEVEL_ID = 1L;
    private static final Long SOLVER_V_ID = 2L;

    @BeforeEach
    void setUp() {
        subQueryService = new SubmissionQueryService(subRepo, scoringLogRepo, teamRepo, eventRepo, storageS, azureBlob);
    }

    @Test
    void getHistoryForTeam_returnsSubmissionsMostRecentFirst() {
        Submission old = buildSubmission(1L, Instant.parse("2026-01-01T00:00:00Z"));
        Submission newer = buildSubmission(2L, Instant.parse("2026-02-01T00:00:00Z"));

        when(subRepo.findByTeamId(TEAM_ID)).thenReturn(List.of(old,newer));

        List<SubmissionResponse> hist = subQueryService.getHistoryForTeam(TEAM_ID);
        
        assertThat(hist).hasSize(2);
        assertThat(hist.get(0).getSubmissionId()).isEqualTo(2L);
        assertThat(hist.get(1).getSubmissionId()).isEqualTo(1L);
    }

    private Submission buildSubmission(Long id, Instant submittedAt) {
        Submission sub = new Submission(TEAM_ID, LEVEL_ID, SOLVER_V_ID, "src/code.zip", "out/output.txt");
        sub.setId(id);
        sub.setScore(new BigDecimal("65.55"));
        sub.setStatus("SCORED");
        sub.setSubmittedAt(submittedAt);
        sub.setOutputFileName("output.txt");
        sub.setSourceFileName("code.zip");
        return sub;
    }
}