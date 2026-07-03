package com.hackathon.platform.storage;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import org.junit.jupiter.api.Test;
 
/** Unit tests for {@link BlobPath} storage key construction. */
class BlobPathTest {

  private static final String HACKATHON_ID = "hackathon-123";
  private static final String LEVEL_ID = "level-456";
  private static final String TEAM_ID = "team-789";
  private static final String SUBMISSION_ID = "sub-101";

  @Test
  void levelFile_returnsCorrectPath() {
    String result = BlobPath.levelFile(HACKATHON_ID, LEVEL_ID, "input.txt");
    assertEquals("hackathons/hackathon-123/levels/level-456/input.txt", result);
  }

  @Test
  void brandingAsset_returnsCorrectPath() {
    String result = BlobPath.brandingAsset(HACKATHON_ID, "logo.png");
    assertEquals("events/hackathon-123/branding/logo.png", result);
  }

  @Test
  void solverFile_returnsCorrectPathWithVersion() {
    String result = BlobPath.solverFile(HACKATHON_ID, 2, "solver.py");
    assertEquals("hackathons/hackathon-123/solver/v2/solver.py", result);
  }

  @Test
  void problemStatement_returnsCorrectPath() {
    String result = BlobPath.problemStatement(HACKATHON_ID, "problem.pdf");
    assertEquals("hackathons/hackathon-123/problem/problem.pdf", result);
  }

  @Test
  void submissionOutput_returnsCorrectPath() {
    String result = BlobPath.submissionOutput(HACKATHON_ID, TEAM_ID, LEVEL_ID, SUBMISSION_ID, "output.txt");
    assertEquals("submissions/hackathon-123/team-789/levels/level-456/sub-101/output/output.txt", result);
  }

  @Test
  void submissionSourceArchive_returnsCorrectPath() {
    String result =
        BlobPath.submissionSourceArchive(HACKATHON_ID, TEAM_ID, LEVEL_ID, SUBMISSION_ID, "source.zip");
    assertEquals("submissions/hackathon-123/team-789/levels/level-456/sub-101/source/source.zip", result);
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
    String result = BlobPath.submissionOutput(EVENT_ID, TEAM_ID, SUBMISSION_ID,
"folder\\file.txt");
    assertFalse(result.contains("\\"));
  }

  @Test
  void solverFile_versionNumberAppearsInPath() {
    String resultV1 = BlobPath.solverFile(HACKATHON_ID, 1, "solver.py");
    String resultV3 = BlobPath.solverFile(HACKATHON_ID, 3, "solver.py");
    assertEquals("hackathons/hackathon-123/solver/v1/solver.py", resultV1);
    assertEquals("hackathons/hackathon-123/solver/v3/solver.py", resultV3);
  }
}
