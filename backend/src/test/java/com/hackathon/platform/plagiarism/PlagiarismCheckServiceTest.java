package com.hackathon.platform.plagiarism;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.dto.PlagiarismDiffResponse;
import com.hackathon.platform.dto.SubmissionSimilarityResponse;
import com.hackathon.platform.model.PlagiarismRun;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.model.SubmissionSimilarity;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.plagiarism.embedding.EmbeddingService;
import com.hackathon.platform.plagiarism.embedding.EmbeddingSimilarityCalculator;
import com.hackathon.platform.plagiarism.embedding.FunctionEmbeddingStore;
import com.hackathon.platform.plagiarism.fingerprint.Winnowing;
import com.hackathon.platform.plagiarism.fingerprint.Winnowing.Fingerprint;
import com.hackathon.platform.plagiarism.fingerprint.Winnowing.FingerprintResult;
import com.hackathon.platform.plagiarism.normalize.NormalizedToken;
import com.hackathon.platform.plagiarism.normalize.StructuralNormalizationResult;
import com.hackathon.platform.plagiarism.normalize.StructuralNormalizer;
import com.hackathon.platform.repository.LeaderboardEntry;
import com.hackathon.platform.repository.LevelRepository;
import com.hackathon.platform.repository.PlagiarismRunRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.repository.SubmissionSimilarityRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.service.StorageService;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlagiarismCheckServiceTest {

  @Mock private SubmissionRepository submissionRepo;
  @Mock private SubmissionSimilarityRepository similarityRepo;
  @Mock private PlagiarismRunRepository runRepo;
  @Mock private LevelRepository levelRepo;
  @Mock private TeamRepository teamRepo;
  @Mock private StorageService storageService;
  @Mock private AzureBlobConfig blobConfig;
  @Mock private StructuralNormalizer structuralNormalizer;
  @Mock private EmbeddingService embeddingService;
  @Mock private FunctionEmbeddingStore functionEmbeddingStore;
  @Mock private EmbeddingSimilarityCalculator embeddingSimilarityCalculator;
  @Mock private Winnowing winnowing;

  private PlagiarismProperties props;
  private PlagiarismCheckService service;

  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final short LEVEL_ID = 2;
  private static final UUID TEAM_A_ID = UUID.randomUUID();
  private static final UUID TEAM_B_ID = UUID.randomUUID();
  private static final UUID TEAM_C_ID = UUID.randomUUID();



  @BeforeEach
  void setUp() {

    props = new PlagiarismProperties();
    props.setMinTokenCount(1);

    service =
        new PlagiarismCheckService(
            submissionRepo,
            similarityRepo,
            runRepo,
            levelRepo,
            teamRepo,
            storageService,
            blobConfig,
            structuralNormalizer,
            embeddingService,
            functionEmbeddingStore,
            embeddingSimilarityCalculator,
            winnowing,
            props);
  }

  private Submission submission(Long id, UUID teamId, String fileName, String storageKey) {

    Submission sub = new Submission(teamId, LEVEL_ID, 1L, storageKey, "out/" + id);
    sub.setId(id);
    sub.setEventId(EVENT_ID);
    sub.setSourceFileName(fileName);
    return sub;

  }

  @Test
  void execute_comparesTopTeamsAndSavesFlaggedPair() {

    PlagiarismRun run = new PlagiarismRun(EVENT_ID, LEVEL_ID, 10, UUID.randomUUID());
    when(runRepo.findById(100L)).thenReturn(Optional.of(run));

    LeaderboardEntry entryA = org.mockito.Mockito.mock(LeaderboardEntry.class);
    when(entryA.getTeamId()).thenReturn(TEAM_A_ID);
    LeaderboardEntry entryB = org.mockito.Mockito.mock(LeaderboardEntry.class);
    when(entryB.getTeamId()).thenReturn(TEAM_B_ID);

    when(submissionRepo.findLeaderboardByEventIdAndLevelId(EVENT_ID, LEVEL_ID))
        .thenReturn(List.of(entryA, entryB));

    Submission subA = submission(1L, TEAM_A_ID, "a.java", "key-a");
    Submission subB = submission(2L, TEAM_B_ID, "b.java", "key-b");

    when(submissionRepo.findBestScoredForTeamsAndLevel(eq(LEVEL_ID), anyList()))
        .thenReturn(List.of(subA, subB));
    
    when(blobConfig.getSubmissionsContainer()).thenReturn("submissions");
    when(storageService.download("submissions", "key-a"))
        .thenReturn(new ByteArrayInputStream("code-a".getBytes(StandardCharsets.UTF_8)));
    when(storageService.download("submissions", "key-b"))
        .thenReturn(new ByteArrayInputStream("code-b".getBytes(StandardCharsets.UTF_8)));

    when(structuralNormalizer.normalize("a.java", "code-a"))
        .thenReturn(
            StructuralNormalizationResult.lexer(
                List.of(new NormalizedToken("a.java", "tok", 0, 3))
            )
        );
    when(structuralNormalizer.normalize("b.java", "code-b"))
        .thenReturn(
            StructuralNormalizationResult.lexer(
                List.of(new NormalizedToken("b.java", "tok", 0, 3))
            )
        );

    FingerprintResult fpA =
        new FingerprintResult(1, Set.of(new Fingerprint(10L, 0), new Fingerprint(20L, 1)));
    
    FingerprintResult fpB =
        new FingerprintResult(1, Set.of(new Fingerprint(20L, 0), new Fingerprint(30L, 1)));
    when(winnowing.fingerprint(anyList(), anyInt(), anyInt())).thenReturn(fpA, fpB);
    when(winnowing.jaccard(any(), any())).thenReturn(0.8);

    when(functionEmbeddingStore.findBySubmissionIds(anyList())).thenReturn(List.of());
    when(embeddingSimilarityCalculator.meanVector(anyList())).thenReturn(new float[0]);
    when(embeddingSimilarityCalculator.centerAll(anyList(), any())).thenReturn(List.of());
    when(embeddingSimilarityCalculator.symmetricBestMatch(anyList(), anyList()))
        .thenReturn(Optional.empty());

    service.execute(100L);

    assertThat(run.getStatus()).isEqualTo("COMPLETED");
    assertThat(run.getPairsCompared()).isEqualTo(1);
    assertThat(run.getPairsFlagged()).isEqualTo(1);
    verify(runRepo, times(2)).save(run);
    verify(similarityRepo).deleteByEventIdAndLevelId(EVENT_ID, LEVEL_ID);
    verify(embeddingService, times(2)).embedAndStore(anyLong(), anyMap(), anyMap());

    ArgumentCaptor<List<SubmissionSimilarity>> captor = ArgumentCaptor.forClass(List.class);
    verify(similarityRepo).saveAll(captor.capture());
    List<SubmissionSimilarity> saved = captor.getValue();
    assertThat(saved).hasSize(1);
    SubmissionSimilarity row = saved.get(0);
    assertThat(row.isFlagged()).isTrue();
    assertThat(row.getMatchedKgramCount()).isEqualTo(1);
    assertThat(row.getSubmissionIdA()).isEqualTo(1L);
    assertThat(row.getSubmissionIdB()).isEqualTo(2L);
    assertThat(row.getTeamIdA()).isEqualTo(TEAM_A_ID);
    assertThat(row.getTeamIdB()).isEqualTo(TEAM_B_ID);
    assertThat(row.getStructuralScore()).isEqualByComparingTo(new BigDecimal("0.8000"));
    assertThat(row.getEmbeddingScore()).isNull();
    assertThat(row.getCombinedScore()).isEqualByComparingTo(new BigDecimal("0.8000"));

  }

  @Test
  void execute_throwsWhenRunNotFound() {

    when(runRepo.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.execute(999L)).isInstanceOf(IllegalArgumentException.class);

  }

  @Test
  void execute_marksRunFailed_whenLevelProcessingThrows() {

    PlagiarismRun run = new PlagiarismRun(EVENT_ID, LEVEL_ID, 10, UUID.randomUUID());
    when(runRepo.findById(200L)).thenReturn(Optional.of(run));
    when(submissionRepo.findLeaderboardByEventIdAndLevelId(EVENT_ID, LEVEL_ID))
        .thenThrow(new RuntimeException("leaderboard query failed"));
    
    service.execute(200L);

    assertThat(run.getStatus()).isEqualTo("FAILED");
    assertThat(run.getErrorMessage()).isEqualTo("leaderboard query failed");
    assertThat(run.getCompletedAt()).isNotNull();
    verify(runRepo, times(2)).save(run);
    verify(similarityRepo, never()).saveAll(any());

  }

  @Test
  void execute_skipsLevel_whenFewerThanTwoScoredSubmissions() {

    PlagiarismRun run = new PlagiarismRun(EVENT_ID, LEVEL_ID, 10, UUID.randomUUID());
    when(runRepo.findById(300L)).thenReturn(Optional.of(run));

    LeaderboardEntry entryA = org.mockito.Mockito.mock(LeaderboardEntry.class);
    when(entryA.getTeamId()).thenReturn(TEAM_A_ID);
    when(submissionRepo.findLeaderboardByEventIdAndLevelId(EVENT_ID, LEVEL_ID))
        .thenReturn(List.of(entryA));

    Submission subA = submission(1L, TEAM_A_ID, "a.java", "key-a");
    when(submissionRepo.findBestScoredForTeamsAndLevel(eq(LEVEL_ID), anyList()))
        .thenReturn(List.of(subA));
    
    service.execute(300L);

    assertThat(run.getStatus()).isEqualTo("COMPLETED");
    assertThat(run.getPairsCompared()).isEqualTo(0);
    assertThat(run.getPairsFlagged()).isEqualTo(0);
    verify(similarityRepo, never())
        .deleteByEventIdAndLevelId(any(), org.mockito.ArgumentMatchers.anyShort());
    verify(similarityRepo, never()).saveAll(any());

  }

  @Test
  void execute_flagsPairBelowAbsoluteThreshold_whenStatisticalOutlierAmongLevelPairs() {

    props.setFlagThreshold(0.99);
    props.setUseRelativeThreshold(true);
    props.setMinPairsForRelativeThreshold(1);
    props.setRelativeThresholdZScore(0.1);
    
    PlagiarismRun run = new PlagiarismRun(EVENT_ID, LEVEL_ID, 10, UUID.randomUUID());
    when(runRepo.findById(400L)).thenReturn(Optional.of(run));

    LeaderboardEntry entryA = org.mockito.Mockito.mock(LeaderboardEntry.class);
    when(entryA.getTeamId()).thenReturn(TEAM_A_ID);
    LeaderboardEntry entryB = org.mockito.Mockito.mock(LeaderboardEntry.class);
    when(entryB.getTeamId()).thenReturn(TEAM_B_ID);
    LeaderboardEntry entryC = org.mockito.Mockito.mock(LeaderboardEntry.class);
    when(entryC.getTeamId()).thenReturn(TEAM_C_ID);

    when(submissionRepo.findLeaderboardByEventIdAndLevelId(EVENT_ID, LEVEL_ID))
        .thenReturn(List.of(entryA, entryB, entryC));

    Submission subA = submission(1L, TEAM_A_ID, "a.java", "key-a");
    Submission subB = submission(2L, TEAM_B_ID, "b.java", "key-b");
    Submission subC = submission(1L, TEAM_C_ID, "c.java", "key-c");

    when(submissionRepo.findBestScoredForTeamsAndLevel(eq(LEVEL_ID), anyList()))
        .thenReturn(List.of(subA, subB, subC));
    
    when(blobConfig.getSubmissionsContainer()).thenReturn("submissions");
    when(storageService.download(eq("submissions"), any()))
        .thenAnswer(inv -> new ByteArrayInputStream("code".getBytes(StandardCharsets.UTF_8)));
    when(structuralNormalizer.normalize(any(), any()))
        .thenReturn(StructuralNormalizationResult.lexer(List.of(new NormalizedToken("f", "tok", 0, 3))));

    FingerprintResult fpA =
        new FingerprintResult(1, Set.of(new Fingerprint(1L, 0)));
    
    FingerprintResult fpB =
        new FingerprintResult(1, Set.of(new Fingerprint(1L, 0)));
    
    FingerprintResult fpC =
        new FingerprintResult(1, Set.of(new Fingerprint(99L, 0)));
    when(winnowing.fingerprint(anyList(), anyInt(), anyInt())).thenReturn(fpA, fpB, fpC);
    when(winnowing.jaccard(any(), any())).thenReturn(0.9, 0.1, 0.1);

    when(functionEmbeddingStore.findBySubmissionIds(anyList())).thenReturn(List.of());
    when(embeddingSimilarityCalculator.meanVector(anyList())).thenReturn(new float[0]);
    when(embeddingSimilarityCalculator.centerAll(anyList(), any())).thenReturn(List.of());
    when(embeddingSimilarityCalculator.symmetricBestMatch(anyList(), anyList()))
        .thenReturn(Optional.empty());

    service.execute(400L);

    ArgumentCaptor<List<SubmissionSimilarity>> captor = ArgumentCaptor.forClass(List.class);
    verify(similarityRepo).saveAll(captor.capture());
    List<SubmissionSimilarity> saved = captor.getValue();

    assertThat(saved).hasSize(3);
    long flaggedCount = saved.stream().filter(SubmissionSimilarity::isFlagged).count();
    assertThat(flaggedCount).isEqualTo(1);
    assertThat(
        saved.stream()
            .filter(SubmissionSimilarity::isFlagged)
            .findFirst()
            .orElseThrow()
            .getCombinedScore()
            .doubleValue()
    )
    .isEqualTo(0.9);

  }

  @Test
  void execute_relativeThresholdDisabled_neverFlagsBelowAbsoluteThreshold() {

    props.setFlagThreshold(1.5);
    props.setUseRelativeThreshold(false);

    PlagiarismRun run = new PlagiarismRun(EVENT_ID, LEVEL_ID, 10, UUID.randomUUID());
    when(runRepo.findById(401L)).thenReturn(Optional.of(run));

    LeaderboardEntry entryA = org.mockito.Mockito.mock(LeaderboardEntry.class);
    when(entryA.getTeamId()).thenReturn(TEAM_A_ID);
    LeaderboardEntry entryB = org.mockito.Mockito.mock(LeaderboardEntry.class);
    when(entryB.getTeamId()).thenReturn(TEAM_B_ID);

    when(submissionRepo.findLeaderboardByEventIdAndLevelId(EVENT_ID, LEVEL_ID))
        .thenReturn(List.of(entryA, entryB));

    Submission subA = submission(1L, TEAM_A_ID, "a.java", "key-a");
    Submission subB = submission(2L, TEAM_B_ID, "b.java", "key-b");

    when(submissionRepo.findBestScoredForTeamsAndLevel(eq(LEVEL_ID), anyList()))
        .thenReturn(List.of(subA, subB));
    
    when(blobConfig.getSubmissionsContainer()).thenReturn("submissions");
    when(storageService.download(eq("submissions"), any()))
        .thenAnswer(inv -> new ByteArrayInputStream("code".getBytes(StandardCharsets.UTF_8)));
    when(structuralNormalizer.normalize(any(), any()))
        .thenReturn(StructuralNormalizationResult.lexer(List.of(new NormalizedToken("f", "tok", 0, 3))));

    FingerprintResult fp =
        new FingerprintResult(1, Set.of(new Fingerprint(10L, 0)));
    
    when(winnowing.fingerprint(anyList(), anyInt(), anyInt())).thenReturn(fp, fp);
    when(winnowing.jaccard(any(), any())).thenReturn(1.0);

    when(functionEmbeddingStore.findBySubmissionIds(anyList())).thenReturn(List.of());
    when(embeddingSimilarityCalculator.meanVector(anyList())).thenReturn(new float[0]);
    when(embeddingSimilarityCalculator.centerAll(anyList(), any())).thenReturn(List.of());
    when(embeddingSimilarityCalculator.symmetricBestMatch(anyList(), anyList()))
        .thenReturn(Optional.empty());

    service.execute(401L);

    ArgumentCaptor<List<SubmissionSimilarity>> captor = ArgumentCaptor.forClass(List.class);
    verify(similarityRepo).saveAll(captor.capture());

    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().get(0).isFlagged()).isFalse();

  }

  @Test
  void execute_zeroTotalWeight_fallsBackToStructuralScoreOnly() {

    props.setStructuralWeight(0.0);
    props.setEmbeddingWeight(0.0);
    props.setFlagThreshold(2.0);
    props.setUseRelativeThreshold(false);

    PlagiarismRun run = new PlagiarismRun(EVENT_ID, LEVEL_ID, 10, UUID.randomUUID());
    when(runRepo.findById(402L)).thenReturn(Optional.of(run));

    LeaderboardEntry entryA = org.mockito.Mockito.mock(LeaderboardEntry.class);
    when(entryA.getTeamId()).thenReturn(TEAM_A_ID);
    LeaderboardEntry entryB = org.mockito.Mockito.mock(LeaderboardEntry.class);
    when(entryB.getTeamId()).thenReturn(TEAM_B_ID);

    when(submissionRepo.findLeaderboardByEventIdAndLevelId(EVENT_ID, LEVEL_ID))
        .thenReturn(List.of(entryA, entryB));

    Submission subA = submission(1L, TEAM_A_ID, "a.java", "key-a");
    Submission subB = submission(2L, TEAM_B_ID, "b.java", "key-b");

    when(submissionRepo.findBestScoredForTeamsAndLevel(eq(LEVEL_ID), anyList()))
        .thenReturn(List.of(subA, subB));
    
    when(blobConfig.getSubmissionsContainer()).thenReturn("submissions");
    when(storageService.download(eq("submissions"), any()))
        .thenAnswer(inv -> new ByteArrayInputStream("code".getBytes(StandardCharsets.UTF_8)));
    when(structuralNormalizer.normalize(any(), any()))
        .thenReturn(StructuralNormalizationResult.lexer(List.of(new NormalizedToken("f", "tok", 0, 3))));

    FingerprintResult fp =
        new FingerprintResult(1, Set.of(new Fingerprint(1L, 0)));
    
    when(winnowing.fingerprint(anyList(), anyInt(), anyInt())).thenReturn(fp, fp);
    when(winnowing.jaccard(any(), any())).thenReturn(0.5);

    when(functionEmbeddingStore.findBySubmissionIds(anyList())).thenReturn(List.of());
    when(embeddingSimilarityCalculator.meanVector(anyList())).thenReturn(new float[0]);
    when(embeddingSimilarityCalculator.centerAll(anyList(), any())).thenReturn(List.of());
    when(embeddingSimilarityCalculator.symmetricBestMatch(anyList(), anyList()))
        .thenReturn(Optional.of(0.9));

    service.execute(402L);

    ArgumentCaptor<List<SubmissionSimilarity>> captor = ArgumentCaptor.forClass(List.class);
    verify(similarityRepo).saveAll(captor.capture());
    SubmissionSimilarity saved = captor.getValue().get(0);

    assertThat(saved.getCombinedScore().doubleValue()).isEqualTo(0.5);
    assertThat(saved.getEmbeddingScore().doubleValue()).isEqualTo(0.9);

  }

  private SubmissionSimilarity row(
    Long subA, Long subB, UUID teamA, UUID teamB, double combined, boolean flagged
  ) {
    BigDecimal score = BigDecimal.valueOf(combined);
    return new SubmissionSimilarity(
        EVENT_ID, LEVEL_ID, subA, subB, teamA, teamB, score, null, score, 5, flagged
    );
  }

  @Test
  void getResults_levelSpecified_returnsRowWithResolvedTeamNames() {
    
    when(similarityRepo.findByEventIdAndLevelIdOrderByCombinedScoreDesc(EVENT_ID, LEVEL_ID))
        .thenReturn(List.of(row(1L, 2L, TEAM_A_ID, TEAM_B_ID, 0.62, true)));
    
    Team teamA = new Team();
    teamA.setTeamName("Alpha");
    Team teamB = new Team();
    teamB.setTeamName("Beta");
    when(teamRepo.findById(TEAM_A_ID)).thenReturn(Optional.of(teamA));
    when(teamRepo.findById(TEAM_B_ID)).thenReturn(Optional.of(teamB));

    List<SubmissionSimilarityResponse> results = service.getResults(EVENT_ID, LEVEL_ID, false);
    
    assertThat(results).hasSize(1);
    SubmissionSimilarityResponse resp = results.get(0);
    assertThat(resp.teamNameA()).isEqualTo("Alpha");
    assertThat(resp.teamNameB()).isEqualTo("Beta");
    assertThat(resp.flagged()).isTrue();

  }

  @Test
  void getResults_unknownTeam_fallsBackToQuestionMark() {

    when(similarityRepo.findByEventIdAndLevelIdOrderByCombinedScoreDesc(EVENT_ID, LEVEL_ID))
        .thenReturn(List.of(row(1L, 2L, TEAM_A_ID, TEAM_B_ID, 0.4, false)));
    when(teamRepo.findById(TEAM_A_ID)).thenReturn(Optional.empty());
    when(teamRepo.findById(TEAM_B_ID)).thenReturn(Optional.empty());

    List<SubmissionSimilarityResponse> results = service.getResults(EVENT_ID, LEVEL_ID, false);

    assertThat(results.get(0).teamNameA()).isEqualTo("?");
    assertThat(results.get(0).teamNameB()).isEqualTo("?");
    
  }

  @Test
  void getResults_levelSpecifiedAndOnlyFlagged_usesFlaggedOnlyQuery() {

    when(similarityRepo.findByEventIdAndLevelIdAndFlaggedTrueOrderByCombinedScoreDesc(
            EVENT_ID, LEVEL_ID
    ))
    .thenReturn(List.of(row(1L, 2L, TEAM_A_ID, TEAM_B_ID, 0.9, true)));
    when(teamRepo.findById(any())).thenReturn(Optional.empty());

    List<SubmissionSimilarityResponse> results = service.getResults(EVENT_ID, LEVEL_ID, true);

    assertThat(results).hasSize(1);
    verify(similarityRepo)
        .findByEventIdAndLevelIdAndFlaggedTrueOrderByCombinedScoreDesc(EVENT_ID, LEVEL_ID);
    verify(similarityRepo, never()).findByEventIdOrderByCombinedScoreDesc(any());

  }

  @Test
  void getResults_noLevel_returnsEveryRowRegardlessOfFlag() {
    when(similarityRepo.findByEventIdOrderByCombinedScoreDesc(EVENT_ID))
        .thenReturn(
            List.of(
                row(1L, 2L, TEAM_A_ID, TEAM_B_ID, 0.9, true),
                row(3L, 4L, TEAM_A_ID, TEAM_B_ID, 0.2, false)
            )
        );
    
    when(teamRepo.findById(any())).thenReturn(Optional.empty());

    List<SubmissionSimilarityResponse> results = service.getResults(EVENT_ID, null, false);

    assertThat(results).hasSize(2);

  }

  @Test
  void getResults_noLevelAndOnlyFlagged_filtersOutUnflaggedRowsManually() {

    when(similarityRepo.findByEventIdOrderByCombinedScoreDesc(EVENT_ID))
        .thenReturn(
            List.of(
                row(1L, 2L, TEAM_A_ID, TEAM_B_ID, 0.9, true),
                row(3L, 4L, TEAM_A_ID, TEAM_B_ID, 0.2, false)
            )
        );
    when(teamRepo.findById(any())).thenReturn(Optional.empty());

    List<SubmissionSimilarityResponse> results = service.getResults(EVENT_ID, null, true);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).flagged()).isTrue();


  }

  @Test
  void getDiff_throwsWhenSubmissionAMissing() {

    when(submissionRepo.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getDiff(1L, 2L))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("1");

  }

  @Test
  void getDiff_noStoredFunctionEmbeddings_returnsNoDataForEitherStatus() {

    props.setKgramSize(1);

    Submission subA = submission(1L, TEAM_A_ID, "a.java", "key-a");
    Submission subB = submission(2L, TEAM_B_ID, "b.java", "key-b");

    when(submissionRepo.findById(1L)).thenReturn(Optional.of(subA));
    when(submissionRepo.findById(2L)).thenReturn(Optional.of(subB));

    when(blobConfig.getSubmissionsContainer()).thenReturn("submissions");
    when(storageService.download("submissions", "key-a"))
        .thenReturn(new ByteArrayInputStream("code-a".getBytes(StandardCharsets.UTF_8)));
    when(storageService.download("submissions", "key-b"))
        .thenReturn(new ByteArrayInputStream("code-b".getBytes(StandardCharsets.UTF_8)));
    
    when(structuralNormalizer.normalize("a.java", "code-a"))
        .thenReturn(
            StructuralNormalizationResult.lexer(
                List.of(new NormalizedToken("a.java", "tok", 0, 3))
            )
        );
    
    when(structuralNormalizer.normalize("b.java", "code-b"))
        .thenReturn(
            StructuralNormalizationResult.lexer(
                List.of(new NormalizedToken("b.java", "tok", 0, 3))
            )
        );
    
    FingerprintResult fpA = new FingerprintResult(1, Set.of(new Fingerprint(10, 0)));
    FingerprintResult fpB = new FingerprintResult(1, Set.of(new Fingerprint(20L, 0)));

    when(winnowing.fingerprint(anyList(), anyInt(), anyInt())).thenReturn(fpA, fpB);
    when(winnowing.jaccard(any(), any())).thenReturn(0.5);

    when(functionEmbeddingStore.findBySubmissionId(1L)).thenReturn(List.of());
    when(functionEmbeddingStore.findBySubmissionId(2L)).thenReturn(List.of());

    PlagiarismDiffResponse diff = service.getDiff(1L, 2L);

    assertThat(diff.submissionIdA()).isEqualTo(1L);
    assertThat(diff.submissionIdB()).isEqualTo(2L);
    assertThat(diff.structuralScore()).isEqualTo(0.5);
    assertThat(diff.matchedRangesA()).isEmpty();
    assertThat(diff.matchedRangesB()).isEmpty();
    assertThat(diff.functionMatches()).isEmpty();
    assertThat(diff.semanticStatus())
        .isEqualTo(PlagiarismDiffResponse.SemanticStatus.NO_DATA_FOR_EITHER);
    assertThat(diff.filesA()).hasSize(1);
    assertThat(diff.filesA().get(0).fileName()).isEqualTo("a.java");

  }


}