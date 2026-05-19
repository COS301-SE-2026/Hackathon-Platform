package com.hackathon.platform.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link BlobPath} storage key construction. */
class BlobPathTest {

  private static final String EVENT_ID = "event-123";
  private static final String LEVEL_ID = "level-456";
  private static final String TEAM_ID = "team-789";
  private static final String SUBMISSION_ID = "sub-101";

  @Test
  void levelFile_returnsCorrectPath() {
    String result = BlobPath.levelFile(EVENT_ID, LEVEL_ID, "input.txt");
    assertEquals("events/event-123/levels/level-456/input.txt", result);
  }

  @Test
  void brandingAsset_returnsCorrectPath() {
    String result = BlobPath.brandingAsset(EVENT_ID, "logo.png");
    assertEquals("events/event-123/branding/logo.png", result);
  }

  @Test
  void solverFile_returnsCorrectPathWithVersion() {
    String result = BlobPath.solverFile(EVENT_ID, 2, "solver.py");
    assertEquals("events/event-123/solver/v2/solver.py", result);
  }

  @Test
  void problemStatement_returnsCorrectPath() {
    String result = BlobPath.problemStatement(EVENT_ID, "problem.pdf");
    assertEquals("events/event-123/problem/problem.pdf", result);
  }

  @Test
  void submissionOutput_returnsCorrectPath() {
    String result = BlobPath.submissionOutput(EVENT_ID, TEAM_ID, SUBMISSION_ID, "output.txt");
    assertEquals("submissions/event-123/team-789/sub-101/output/output.txt", result);
  }

  @Test
  void submissionSourceArchive_returnsCorrectPath() {
    String result =
        BlobPath.submissionSourceArchive(EVENT_ID, TEAM_ID, SUBMISSION_ID, "source.zip");
    assertEquals("submissions/event-123/team-789/sub-101/source/source.zip", result);
  }

  @Test
  void scoringLog_returnsCorrectPath() {
    String result = BlobPath.scoringLog(EVENT_ID, SUBMISSION_ID, "log.txt");
    assertEquals("logs/event-123/sub-101/log.txt", result);
  }

  @Test
  void levelFile_sanitisesPathTraversal() {
    String result = BlobPath.levelFile(EVENT_ID, LEVEL_ID, "../../../etc/passwd");
    assertFalse(result.contains(".."));
  }

  @Test
  void submissionOutput_sanitisesBackslash() {
    String result =
        BlobPath.submissionOutput(EVENT_ID, TEAM_ID, SUBMISSION_ID, "folder\\file.txt");
    assertFalse(result.contains("\\"));
  }

  @Test
  void solverFile_versionNumberAppearsInPath() {
    String resultV1 = BlobPath.solverFile(EVENT_ID, 1, "solver.py");
    String resultV3 = BlobPath.solverFile(EVENT_ID, 3, "solver.py");
    assertEquals("events/event-123/solver/v1/solver.py", resultV1);
    assertEquals("events/event-123/solver/v3/solver.py", resultV3);
  }
}