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

  @Test
  void saveSolverVersion_savesAndReturnsSolverVersion() {
    SolverVersion expected = new SolverVersion(EVENT_ID, UPLOADED_BY, "events/.../solver.py");
    when(solverVersionRepository.save(any(SolverVersion.class))).thenReturn(expected);

    SolverVersion result =
        fileMetadataService.saveSolverVersion(
            EVENT_ID, UPLOADED_BY, "events/.../solver.py", 1, "solver.py", 2048L);

    assertThat(result).isEqualTo(expected);
    verify(solverVersionRepository).save(any(SolverVersion.class));
  }

  @Test
  void saveSubmission_savesTwiceAndBuildsCanonicalKeysUsingRealId() {
    Submission firstSave =
        new Submission(TEAM_ID, LEVEL_ID, SOLVER_VERSION_ID, "pending", "pending");
    firstSave.setId(SUBMISSION_ID);

    when(submissionRepository.save(any(Submission.class)))
        .thenReturn(firstSave)
        .thenAnswer(invocation -> invocation.getArgument(0));

    Submission result =
        fileMetadataService.saveSubmission(
            EVENT_ID_STR,
            TEAM_ID,
            LEVEL_ID,
            SOLVER_VERSION_ID,
            "output.txt",
            512L,
            "text/plain",
            "archive.zip",
            4096L,
            "application/zip");

    verify(submissionRepository, times(2)).save(any(Submission.class));

    assertThat(result.getOutputStorageKey())
        .isEqualTo(
            "submissions/"
                + EVENT_ID_STR
                + "/"
                + TEAM_ID
                + "/"
                + SUBMISSION_ID
                + "/output/output.txt");
    assertThat(result.getSourceCodeStorageKey())
        .isEqualTo(
            "submissions/"
                + EVENT_ID_STR
                + "/"
                + TEAM_ID
                + "/"
                + SUBMISSION_ID
                + "/source/archive.zip");
    assertThat(result.getStatus()).isEqualTo("QUEUED");
  }

  @Test
  void getLevelFileStorageKey_returnsStorageKeyWhenFound() {
    LevelFile levelFile = new LevelFile(LEVEL_ID, "test.pdf", "events/.../test.pdf", "PDF");
    when(levelFileRepository.findByLevelIdAndFileName(LEVEL_ID, "test.pdf"))
        .thenReturn(Optional.of(levelFile));

    String result = fileMetadataService.getLevelFileStorageKey(LEVEL_ID, "test.pdf");

    assertThat(result).isEqualTo("events/.../test.pdf");
  }

  @Test
  void getLevelFileStorageKey_throwsWhenNotFound() {
    when(levelFileRepository.findByLevelIdAndFileName(LEVEL_ID, "missing.pdf"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> fileMetadataService.getLevelFileStorageKey(LEVEL_ID, "missing.pdf"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Level file not found");
  }

  @Test
  void getSubmissionOutputStorageKey_returnsKeyWhenFound() {
    Submission submission =
        new Submission(
            TEAM_ID,
            LEVEL_ID,
            SOLVER_VERSION_ID,
            "submissions/.../source/archive.zip",
            "submissions/.../output/output.txt");
    when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));

    String result = fileMetadataService.getSubmissionOutputStorageKey(SUBMISSION_ID);

    assertThat(result).isEqualTo("submissions/.../output/output.txt");
  }

  @Test
  void getSubmissionOutputStorageKey_throwsWhenNotFound() {
    when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> fileMetadataService.getSubmissionOutputStorageKey(SUBMISSION_ID))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Submission not found");
  }

  @Test
  void getSubmissionSourceStorageKey_returnsKeyWhenFound() {
    Submission submission =
        new Submission(
            TEAM_ID,
            LEVEL_ID,
            SOLVER_VERSION_ID,
            "submissions/.../source/archive.zip",
            "submissions/.../output/output.txt");
    when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));

    String result = fileMetadataService.getSubmissionSourceStorageKey(SUBMISSION_ID);

    assertThat(result).isEqualTo("submissions/.../source/archive.zip");
  }

  @Test
  void getSubmissionSourceStorageKey_throwsWhenNotFound() {
    when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> fileMetadataService.getSubmissionSourceStorageKey(SUBMISSION_ID))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Submission not found");
  }
}
