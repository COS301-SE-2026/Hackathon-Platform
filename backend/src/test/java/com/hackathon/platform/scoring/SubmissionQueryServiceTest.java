package com.hackathon.platform.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.hackathon.platform.storage.StorageException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SubmissionQueryServiceTest {
  @Mock private SubmissionRepository subRepo;
  @Mock private ScoringLogRepository scoringLogRepo;
  @Mock private TeamRepository teamRepo;
  @Mock private EventRepository eventRepo;
  @Mock private StorageService storageService;
  @Mock private AzureBlobConfig azureBlob;

  private SubmissionQueryService subQueryService;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final short LEVEL_ID = 1;
  private static final Long SOLVER_V_ID = 2L;
  private static final int LIMIT = 5;

  @BeforeEach
  void setUp() {
    subQueryService =
        new SubmissionQueryService(
            subRepo, scoringLogRepo, teamRepo, eventRepo, storageService, azureBlob);
  }

  @Test
  void getHistoryForTeam_returnsSubmissionsMostRecentFirst() {
    Submission old = buildSubmission(1L, Instant.parse("2026-01-01T00:00:00Z"));
    Submission newer = buildSubmission(2L, Instant.parse("2026-02-01T00:00:00Z"));

    when(subRepo.findByTeamId(TEAM_ID)).thenReturn(List.of(old, newer));

    List<SubmissionResponse> hist = subQueryService.getHistoryForTeam(TEAM_ID);

    assertThat(hist).hasSize(2);
    assertThat(hist.get(0).getSubmissionId()).isEqualTo(2L);
    assertThat(hist.get(1).getSubmissionId()).isEqualTo(1L);
  }

  @Test
  void getHistoryForTeam_returnsEmptyListWhenNoSubmissions() {
    when(subRepo.findByTeamId(TEAM_ID)).thenReturn(List.of());

    List<SubmissionResponse> hist = subQueryService.getHistoryForTeam(TEAM_ID);

    assertThat(hist).isEmpty();
  }

  @Test
  void getHistoryForTeamAndLevel_returnsSubmissions() {
    Submission sub1 = buildSubmission(1L, Instant.parse("2026-01-01T00:00:00Z"));
    Submission sub2 = buildSubmission(2L, Instant.parse("2026-02-01T00:00:00Z"));

    when(subRepo.findLatestByTeamAndLevel(TEAM_ID, LEVEL_ID)).thenReturn(List.of(sub1, sub2));

    List<SubmissionResponse> result = subQueryService.getHistoryForTeamAndLevel(TEAM_ID, LEVEL_ID);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getSubmissionId()).isEqualTo(1L);
  }

  @Test
  void getHistoryForTeamAndLevel_returnsEmptyListWhenNoSubmissions() {
    when(subRepo.findLatestByTeamAndLevel(TEAM_ID, LEVEL_ID)).thenReturn(List.of());

    List<SubmissionResponse> result = subQueryService.getHistoryForTeamAndLevel(TEAM_ID, LEVEL_ID);

    assertThat(result).isEmpty();
  }

  @Test
  void getRecentSubmissions_returnsCorrect() {
    Submission old = buildSubmission(1L, Instant.parse("2026-01-01T00:00:00Z"));
    Submission newer = buildSubmission(2L, Instant.parse("2026-02-01T00:00:00Z"));
    when(subRepo.getRecentSubmissions(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(old, newer));
    List<SubmissionResponse> r = subQueryService.getRecentSubmissions(USER_ID, LIMIT);

    assertThat(r).hasSize(2);
    assertThat(r.get(0).getSubmissionId()).isEqualTo(1L);
    assertThat(r.get(1).getSubmissionId()).isEqualTo(2L);
  }

  @Test
  void getRecentSubmissions_returnsEmptyListWhenNoSubmissions() {
    when(subRepo.getRecentSubmissions(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

    List<SubmissionResponse> result = subQueryService.getRecentSubmissions(USER_ID, LIMIT);

    assertThat(result).isEmpty();
  }

  @Test
  void getHistoryForTeam_doesNotEagerlyLoadLogs() {
    Submission sub = buildSubmission(1L, Instant.parse("2026-01-01T00:00:00Z"));

    when(subRepo.findByTeamId(TEAM_ID)).thenReturn(List.of(sub));

    List<SubmissionResponse> hist = subQueryService.getHistoryForTeam(TEAM_ID);

    assertThat(hist).hasSize(1);
    assertThat(hist.get(0).getScoringLog()).isNull();

    verifyNoInteractions(scoringLogRepo);
  }

  @Test
  void getRecentSubmission_doesntLoadLogs() {
    Submission sub = buildSubmission(1L, Instant.parse("2026-01-01T00:00:00Z"));
    when(subRepo.getRecentSubmissions(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(sub));

    List<SubmissionResponse> hist = subQueryService.getRecentSubmissions(USER_ID, LIMIT);

    assertThat(hist).hasSize(1);
    assertThat(hist.get(0).getScoringLog()).isNull();

    verifyNoInteractions(scoringLogRepo);
  }

  @Test
  void getSubmissionDetail_returnsSubmissionWithLogWhenExists() throws IOException {
    Submission sub = buildSubmission(5L, Instant.parse("2026-01-01T00:00:00Z"));
    ScoringLog log = buildScoringLog(5L, "log-key-123");
    String logContent = "Test log content";

    when(subRepo.findByIdAndTeamId(5L, TEAM_ID)).thenReturn(Optional.of(sub));
    when(scoringLogRepo.findBySubmissionId(5L)).thenReturn(Optional.of(log));
    when(azureBlob.getScoringLogsContainer()).thenReturn("container");
    when(storageService.download(eq("container"), eq("log-key-123")))
        .thenReturn(new ByteArrayInputStream(logContent.getBytes()));

    SubmissionResponse res = subQueryService.getSubmissionDetail(5L, TEAM_ID);

    assertThat(res).isNotNull();
    assertThat(res.getSubmissionId()).isEqualTo(5L);
    assertThat(res.getScoringLog()).isNotNull();
    assertThat(res.getScoringLog().getLogContent()).isEqualTo(logContent);
  }

  @Test
  void getSubmissionDetail_returnsNoLogWhenNoneExists() {
    Submission sub = buildSubmission(5L, Instant.parse("2026-01-01T00:00:00Z"));

    when(subRepo.findByIdAndTeamId(5L, TEAM_ID)).thenReturn(Optional.of(sub));
    when(scoringLogRepo.findBySubmissionId(5L)).thenReturn(Optional.empty());

    SubmissionResponse res = subQueryService.getSubmissionDetail(5L, TEAM_ID);

    assertThat(res).isNotNull();
    assertThat(res.getScoringLog()).isNull();
  }

  @Test
  void getSubmissionDetails_forWrongTeam_throwsIllegalArgumentException() {
    when(subRepo.findByIdAndTeamId(5L, TEAM_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> subQueryService.getSubmissionDetail(5L, TEAM_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("The submission could not be for this team: ");
  }

  @Test
  void getSubmissionDetailForAdmin_returnsSubmissionWithLog() throws IOException {
    Submission sub = buildSubmission(9L, Instant.parse("2026-01-01T00:00:00Z"));
    ScoringLog log = buildScoringLog(9L, "admin-log-key");
    String logContent = "Admin log content";

    when(subRepo.findById(9L)).thenReturn(Optional.of(sub));
    when(scoringLogRepo.findBySubmissionId(9L)).thenReturn(Optional.of(log));
    when(azureBlob.getScoringLogsContainer()).thenReturn("container");
    when(storageService.download(eq("container"), eq("admin-log-key")))
        .thenReturn(new ByteArrayInputStream(logContent.getBytes()));

    SubmissionResponse res = subQueryService.getSubmissionDetailForAdmin(9L);

    assertThat(res.getSubmissionId()).isEqualTo(9L);
    assertThat(res.getScoringLog()).isNotNull();
    assertThat(res.getScoringLog().getLogContent()).isEqualTo(logContent);
  }

  private Submission buildSubmission(Long id, Instant submittedAt) {
    Submission sub =
        new Submission(TEAM_ID, LEVEL_ID, SOLVER_V_ID, "src/code.zip", "out/output.txt");
    sub.setId(id);
    sub.setScore(new BigDecimal("65.55"));
    sub.setStatus("SCORED");
    sub.setSubmittedAt(submittedAt);
    sub.setOutputFileName("output.txt");
    sub.setSourceFileName("code.zip");
    return sub;
  }

  private ScoringLog buildScoringLog(Long submissionId, String storageKey) {
    ScoringLog log = new ScoringLog();
    log.setSubmissionId(submissionId);
    log.setTeamId(TEAM_ID);
    log.setEventId(EVENT_ID);
    log.setStorageKey(storageKey);
    log.setCreatedAt(Instant.now());
    return log;
  }
}