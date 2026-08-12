package com.hackathon.platform.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.model.Level;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.model.ScoringLog;
import com.hackathon.platform.model.SolverVersion;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.repository.LevelFileRepository;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.LevelRepository;
import com.hackathon.platform.repository.ScoringLogRepository;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.service.StorageService;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {
  @Mock private SubmissionRepository subRepo;
  @Mock private SolverVersionRepository solverVRepo;
  @Mock private LevelFileRepository lvlFileRepo;
  @Mock private LevelRepository levelRepo;
  @Mock private ScoringLogRepository scoringLogRepo;
  @Mock private TeamRepository teamRepo;
  @Mock private StorageService storageS;
  @Mock private AzureBlobConfig azure;
  @Mock private SolverRunner solverRunner;
  @Mock private LeaderboardUpdateService leaderboardUpdateService;
  @Mock private EventRepository eventRepo;

  private ScoringService scoringService;

  private static final Long SUB_ID = 1L;
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final short LVL_ID = 2;
  private static final Long SOLVER_V_ID = 3L;

  private Submission sub;
  private Team team;
  private SolverVersion solverVersion;
  private Level level;

  @BeforeEach
  void setUp() {
    scoringService =
        new ScoringService(
            subRepo,
            solverVRepo,
            lvlFileRepo,
            levelRepo,
            scoringLogRepo,
            teamRepo,
            storageS,
            azure,
            solverRunner,
            leaderboardUpdateService,
            eventRepo);

    sub = new Submission(TEAM_ID, LVL_ID, SOLVER_V_ID, "src/code.zip", "out/output.txt");
    sub.setId(SUB_ID);
    sub.setEventId(EVENT_ID);
    sub.setOutputFileName("output.txt");

    team = new Team();
    team.setTeamId(TEAM_ID);
    team.setTeamName("test team");
    team.setCreatedByUserId(UUID.randomUUID());
    team.setEventId(EVENT_ID);

    solverVersion = new SolverVersion(UUID.randomUUID(), UUID.randomUUID(), "events/.../solver.py");
    solverVersion.setId(SOLVER_V_ID);

    level = new Level(EVENT_ID, "Level Two", LVL_ID);
    level.setId(LVL_ID);
  }

  private void stubContainers() {
    when(azure.getEventResourcesContainer()).thenReturn("event-resources");
    when(azure.getSubmissionsContainer()).thenReturn("submissions");
    when(azure.getScoringLogsContainer()).thenReturn("scoring-logs");
  }

  private void stubCoreLookups() {
    when(subRepo.findById(SUB_ID)).thenReturn(Optional.of(sub));
    when(teamRepo.findById(TEAM_ID)).thenReturn(Optional.of(team));
    when(solverVRepo.findById(SOLVER_V_ID)).thenReturn(Optional.of(solverVersion));
    when(levelRepo.findById(LVL_ID)).thenReturn(Optional.of(level));
    when(lvlFileRepo.findByLevelId((long) LVL_ID)).thenReturn(List.of());
  }

  @Test
  void scoreSubmission_withSuccessfulSolverRun_setsScoreAndStatus() {
    stubContainers();
    stubCoreLookups();

    when(storageS.download(eq("event-resources"), any()))
        .thenReturn(new ByteArrayInputStream("Solver code".getBytes()));
    when(storageS.download(eq("submissions"), any()))
        .thenReturn(new ByteArrayInputStream("output data".getBytes()));

    SolverResult res =
        new SolverResult(new BigDecimal("60.66"), "SCORED", null, List.of("validated"));
    when(solverRunner.run(any(), any(), any(), any()))
        .thenReturn(new SolverRunOutcome(res, "log line \n {...}", ""));

    when(subRepo.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));
    when(scoringLogRepo.findBySubmissionId(SUB_ID)).thenReturn(Optional.empty());

    Submission scored = scoringService.scoreSubmission(SUB_ID);

    assertThat(scored.getScore()).isEqualByComparingTo("60.66");
    assertThat(scored.getStatus()).isEqualTo("SCORED");
    verify(scoringLogRepo).save(any());
    verify(leaderboardUpdateService)
        .pushLeaderboardUpdate(eq(EVENT_ID), eq((long) LVL_ID), eq(TEAM_ID), eq(SUB_ID));
  }

  @Test
  void scoreSubmission_writesLogToASubmissionScopedBlobKey() {
    stubContainers();
    stubCoreLookups();

    when(storageS.download(eq("event-resources"), any()))
        .thenReturn(new ByteArrayInputStream("Solver code".getBytes()));
    when(storageS.download(eq("submissions"), any()))
        .thenReturn(new ByteArrayInputStream("output data".getBytes()));

    SolverResult res = new SolverResult(new BigDecimal("60.66"), "SCORED", null, List.of());
    when(solverRunner.run(any(), any(), any(), any()))
        .thenReturn(new SolverRunOutcome(res, "{}", ""));
    when(subRepo.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));
    when(scoringLogRepo.findBySubmissionId(SUB_ID)).thenReturn(Optional.empty());

    scoringService.scoreSubmission(SUB_ID);

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageS).uploadBytes(eq("scoring-logs"), keyCaptor.capture(), any(), eq("text/plain"));

    // the key must be scoped to this submission id, so re-scoring another submission never
    // touches this one's blob
    assertThat(keyCaptor.getValue()).contains("/" + SUB_ID + "/");
    assertThat(keyCaptor.getValue()).endsWith("scoring_log.txt");
  }

  @Test
  void scoreSubmission_reScoringReusesTheExistingScoringLogRowInsteadOfDuplicating() {
    stubContainers();
    stubCoreLookups();

    when(storageS.download(eq("event-resources"), any()))
        .thenReturn(new ByteArrayInputStream("Solver code".getBytes()));
    when(storageS.download(eq("submissions"), any()))
        .thenReturn(new ByteArrayInputStream("output data".getBytes()));

    SolverResult res = new SolverResult(new BigDecimal("40.00"), "SCORED", null, List.of());
    when(solverRunner.run(any(), any(), any(), any()))
        .thenReturn(new SolverRunOutcome(res, "{}", ""));
    when(subRepo.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

    ScoringLog existingLog =
        new ScoringLog(SUB_ID, TEAM_ID, EVENT_ID, (long) LVL_ID, "logs/old/key/scoring_log.txt");
    when(scoringLogRepo.findBySubmissionId(SUB_ID)).thenReturn(Optional.of(existingLog));
    when(scoringLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    scoringService.scoreSubmission(SUB_ID);

    ArgumentCaptor<ScoringLog> logCaptor = ArgumentCaptor.forClass(ScoringLog.class);
    verify(scoringLogRepo, times(1)).save(logCaptor.capture());
    assertThat(logCaptor.getValue().getSubmissionId()).isEqualTo(SUB_ID);
  }

  @Test
  void scoreSubmission_withSolverFailure_marksFailedAndLogsReason() {
    stubContainers();
    stubCoreLookups();

    when(storageS.download(eq("event-resources"), any()))
        .thenReturn(new ByteArrayInputStream("Solver code".getBytes()));
    when(storageS.download(eq("submissions"), any()))
        .thenReturn(new ByteArrayInputStream("output data".getBytes()));

    when(solverRunner.run(any(), any(), any(), any()))
        .thenThrow(new SolverExecutionException("SolverExceeded time limit", "TIMEOUT"));

    when(subRepo.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));
    when(scoringLogRepo.findBySubmissionId(SUB_ID)).thenReturn(Optional.empty());

    Submission scored = scoringService.scoreSubmission(SUB_ID);

    assertThat(scored.getStatus()).isEqualTo("FAILED");
    assertThat(scored.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
    verify(scoringLogRepo).save(any());
    verify(leaderboardUpdateService, never()).pushLeaderboardUpdate(any(), any(), any(), any());
  }

  @Test
  void scoreSubmission_downloadsLevelInputsWhenLevelFilesExist() {
    stubContainers();
    LevelFile inputFile = new LevelFile((long) LVL_ID, "input.txt", "events/.../input.txt", "TXT");
    when(subRepo.findById(SUB_ID)).thenReturn(Optional.of(sub));
    when(teamRepo.findById(TEAM_ID)).thenReturn(Optional.of(team));
    when(solverVRepo.findById(SOLVER_V_ID)).thenReturn(Optional.of(solverVersion));
    when(levelRepo.findById(LVL_ID)).thenReturn(Optional.of(level));
    when(lvlFileRepo.findByLevelId((long) LVL_ID)).thenReturn(List.of(inputFile));

    when(storageS.download(eq("event-resources"), anyString()))
        .thenReturn(new ByteArrayInputStream("data".getBytes()));
    when(storageS.download(eq("submissions"), anyString()))
        .thenReturn(new ByteArrayInputStream("output".getBytes()));

    SolverResult res = new SolverResult(new BigDecimal("60.66"), "SCORED", null, List.of());
    when(solverRunner.run(any(), any(), any(), any()))
        .thenReturn(new SolverRunOutcome(res, "{}", ""));
    when(subRepo.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));
    when(scoringLogRepo.findBySubmissionId(SUB_ID)).thenReturn(Optional.empty());

    scoringService.scoreSubmission(SUB_ID);

    verify(solverRunner).run(any(Path.class), any(Path.class), any(Path.class), any());
  }

  @Test
  void scoreSubmission_withUnknownSubmission_throwsIllegalArgumentException() {
    when(subRepo.findById(SUB_ID)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> scoringService.scoreSubmission(SUB_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("wasnt found");

    verify(subRepo, never()).save(any());
  }
}
