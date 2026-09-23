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


}