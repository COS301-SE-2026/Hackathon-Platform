package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.model.SolverVersion;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.LevelFileRepository;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileMetadataServiceTest {

  @Mock private LevelFileRepository levelFileRepository;
  @Mock private SolverVersionRepository solverVersionRepository;
  @Mock private SubmissionRepository submissionRepository;

  private FileMetadataService fileMetadataService;

  private static final Long LEVEL_ID = 1L;
  private static final String EVENT_ID_STR = "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13";
  private static final UUID EVENT_ID = UUID.fromString(EVENT_ID_STR);
  private static final UUID UPLOADED_BY = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final Long SOLVER_VERSION_ID = 1L;
  private static final Long SUBMISSION_ID = 6L;

  @BeforeEach
  void setUp() {
    fileMetadataService =
        new FileMetadataService(levelFileRepository, solverVersionRepository, submissionRepository);
  }

  @Test
  void saveLevelFile_savesAndReturnsLevelFile() {
    LevelFile expected = new LevelFile(LEVEL_ID, "test.pdf", "events/.../test.pdf", "PDF");
    when(levelFileRepository.save(any(LevelFile.class))).thenReturn(expected);

    LevelFile result =
        fileMetadataService.saveLevelFile(
            LEVEL_ID, "test.pdf", "events/.../test.pdf", "PDF", 1024L, "application/pdf");

    assertThat(result).isEqualTo(expected);
    verify(levelFileRepository).save(any(LevelFile.class));
  }

  
}
