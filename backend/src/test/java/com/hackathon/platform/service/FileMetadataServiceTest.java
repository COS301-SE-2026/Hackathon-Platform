package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.model.Hackathon;
import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.model.SolverVersion;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.HackathonRepository;
import com.hackathon.platform.repository.LevelFileRepository;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import java.util.List;
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
  @Mock private HackathonRepository hackathonRepository;

  private FileMetadataService fileMetadataService;

  private static final short LEVEL_ID = 1;
  private static final Long LEVEL_ID_LONG = 1L;
  private static final String EVENT_ID_STR = "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13";
  private static final UUID EVENT_ID = UUID.fromString(EVENT_ID_STR);
  private static final UUID UPLOADED_BY = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final Long SOLVER_VERSION_ID = 1L;
  private static final Long SUBMISSION_ID = 6L;

  @BeforeEach
  void setUp() {
    fileMetadataService =
        new FileMetadataService(
            levelFileRepository,
            solverVersionRepository,
            submissionRepository,
            hackathonRepository);
  }

  @Test
  void saveLevelFile_savesAndReturnsLevelFile() {
    LevelFile expected = new LevelFile((long) LEVEL_ID, "test.pdf", "events/.../test.pdf", "PDF");
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
            EVENT_ID, UPLOADED_BY, "events/.../solver.py", 1, "solver.py", 2048L, null);

    assertThat(result).isEqualTo(expected);
    verify(solverVersionRepository).save(any(SolverVersion.class));
  }

  @Test
  void saveSolverVersion_withNotes_savesAndReturnsSolverVersion() {
    SolverVersion expected = new SolverVersion(EVENT_ID, UPLOADED_BY, "events/.../solver.py");
    when(solverVersionRepository.save(any(SolverVersion.class))).thenReturn(expected);

    SolverVersion result =
        fileMetadataService.saveSolverVersion(
            EVENT_ID,
            UPLOADED_BY,
            "events/.../solver.py",
            1,
            "solver.py",
            2048L,
            "Initial version");

    assertThat(result).isEqualTo(expected);
    verify(solverVersionRepository).save(any(SolverVersion.class));
  }

  @Test
  void saveSubmission_savesTwiceAndBuildsCanonicalKeysUsingRealId() {
    Submission firstSave =
        new Submission(TEAM_ID, LEVEL_ID, SOLVER_VERSION_ID, "pending", "pending");
    firstSave.setId(SUBMISSION_ID);
    firstSave.setEventId(EVENT_ID);

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
                + "/levels/"
                + LEVEL_ID
                + "/"
                + SUBMISSION_ID
                + "/output/output.txt");
    assertThat(result.getSourceCodeStorageKey())
        .isEqualTo(
            "submissions/"
                + EVENT_ID_STR
                + "/"
                + TEAM_ID
                + "/levels/"
                + LEVEL_ID
                + "/"
                + SUBMISSION_ID
                + "/source/archive.zip");
    assertThat(result.getStatus()).isEqualTo("QUEUED");
    assertThat(result.getEventId()).isEqualTo(EVENT_ID);
  }

  @Test
  void saveSubmission_outputKeyEndsWithOutputFileName() {
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
            "my_output.txt",
            512L,
            "text/plain",
            "my_archive.zip",
            4096L,
            "application/zip");

    assertThat(result.getOutputStorageKey()).endsWith("/my_output.txt");
    assertThat(result.getSourceCodeStorageKey()).endsWith("/my_archive.zip");
  }

  @Test
  void saveSubmission_differentLevelIdsProduceDifferentStorageKeys() {
    Submission firstSaveA =
        new Submission(TEAM_ID, (short) 1, SOLVER_VERSION_ID, "pending", "pending");
    firstSaveA.setId(SUBMISSION_ID);
    when(submissionRepository.save(any(Submission.class)))
        .thenReturn(firstSaveA)
        .thenAnswer(invocation -> invocation.getArgument(0));

    Submission resultLevelOne =
        fileMetadataService.saveSubmission(
            EVENT_ID_STR,
            TEAM_ID,
            (short) 1,
            SOLVER_VERSION_ID,
            "output.txt",
            512L,
            "text/plain",
            "archive.zip",
            4096L,
            "application/zip");

    Submission firstSaveB =
        new Submission(TEAM_ID, (short) 2, SOLVER_VERSION_ID, "pending", "pending");
    firstSaveB.setId(SUBMISSION_ID);
    when(submissionRepository.save(any(Submission.class)))
        .thenReturn(firstSaveB)
        .thenAnswer(invocation -> invocation.getArgument(0));

    Submission resultLevelTwo =
        fileMetadataService.saveSubmission(
            EVENT_ID_STR,
            TEAM_ID,
            (short) 2,
            SOLVER_VERSION_ID,
            "output.txt",
            512L,
            "text/plain",
            "archive.zip",
            4096L,
            "application/zip");

    assertThat(resultLevelOne.getOutputStorageKey())
        .isNotEqualTo(resultLevelTwo.getOutputStorageKey());
  }

  @Test
  void updateProblemStatementStorageKey_updatesAndReturnsHackathon() {
    Hackathon hackathon = new Hackathon();
    hackathon.setHackathonId(EVENT_ID);
    hackathon.setName("Test Hackathon");

    when(hackathonRepository.findById(EVENT_ID)).thenReturn(Optional.of(hackathon));
    when(hackathonRepository.save(any(Hackathon.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Hackathon result =
        fileMetadataService.updateProblemStatementStorageKey(
            EVENT_ID, "hackathons/.../problem/spec.pdf");

    assertThat(result.getProblemStatementStorageKey()).isEqualTo("hackathons/.../problem/spec.pdf");
    verify(hackathonRepository).save(hackathon);
  }

  @Test
  void updateProblemStatementStorageKey_throwsWhenHackathonNotFound() {
    when(hackathonRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                fileMetadataService.updateProblemStatementStorageKey(
                    EVENT_ID, "hackathons/.../problem/spec.pdf"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Hackathon not found");

    verify(hackathonRepository, never()).save(any());
  }

  @Test
  void listLevelFiles_returnsFilesForLevel() {
    LevelFile fileA = new LevelFile(LEVEL_ID_LONG, "a.pdf", "hackathons/.../a.pdf", "PDF");
    LevelFile fileB = new LevelFile(LEVEL_ID_LONG, "b.pdf", "hackathons/.../b.pdf", "PDF");
    when(levelFileRepository.findByLevelId(LEVEL_ID_LONG)).thenReturn(List.of(fileA, fileB));

    List<LevelFile> result = fileMetadataService.listLevelFiles(LEVEL_ID_LONG);

    assertThat(result).containsExactly(fileA, fileB);
  }

  @Test
  void getLevelFile_returnsFileWhenFound() {
    LevelFile file = new LevelFile(LEVEL_ID_LONG, "test.pdf", "events/.../test.pdf", "PDF");
    when(levelFileRepository.findById(10L)).thenReturn(Optional.of(file));

    LevelFile result = fileMetadataService.getLevelFile(10L);

    assertThat(result).isEqualTo(file);
  }

  @Test
  void getLevelFile_throwsWhenNotFound() {
    when(levelFileRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> fileMetadataService.getLevelFile(99L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Level file not found");
  }

  @Test
  void deleteLevelFile_deletesWhenFileExists() {
    when(levelFileRepository.existsById(10L)).thenReturn(true);

    fileMetadataService.deleteLevelFile(10L);

    verify(levelFileRepository).deleteById(10L);
  }

  @Test
  void deleteLevelFile_throwsWhenNotFound() {
    when(levelFileRepository.existsById(99L)).thenReturn(false);

    assertThatThrownBy(() -> fileMetadataService.deleteLevelFile(99L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Level file not found");

    verify(levelFileRepository, never()).deleteById(any());
  }

  @Test
  void getLevelFileStorageKey_returnsStorageKeyWhenFound() {
    LevelFile levelFile = new LevelFile(LEVEL_ID_LONG, "test.pdf", "events/.../test.pdf", "PDF");
    when(levelFileRepository.findByLevelIdAndFileName(LEVEL_ID_LONG, "test.pdf"))
        .thenReturn(Optional.of(levelFile));

    String result = fileMetadataService.getLevelFileStorageKey(LEVEL_ID_LONG, "test.pdf");

    assertThat(result).isEqualTo("events/.../test.pdf");
  }

  @Test
  void getLevelFileStorageKey_throwsWhenNotFound() {
    when(levelFileRepository.findByLevelIdAndFileName(LEVEL_ID_LONG, "missing.pdf"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> fileMetadataService.getLevelFileStorageKey(LEVEL_ID_LONG, "missing.pdf"))
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
