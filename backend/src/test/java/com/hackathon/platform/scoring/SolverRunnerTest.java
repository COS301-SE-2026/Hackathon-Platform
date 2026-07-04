package com.hackathon.platform.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.platform.config.SolverExecutionConfig;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises {@link SolverRunner} against real, tiny throwaway Python scripts rather than mocking
 * the process.
 */
class SolverRunnerTest {

  @TempDir Path tempDir;

  private SolverRunner solverRunner;
  private Path outputFile;

  @BeforeEach
  void setUp() throws IOException {
    SolverExecutionConfig config = new SolverExecutionConfig();
    setField(config, "timeoutSeconds", 5);
    setField(config, "pythonExecutable", "python");
    setField(config, "workDir", tempDir.resolve("scratch").toString());
    setField(config, "maxOutputBytes", 1024 * 1024);

    solverRunner = new SolverRunner(config);

    outputFile = tempDir.resolve("output.txt");
    Files.writeString(outputFile, "42\n");
  }

  @AfterEach
  void tearDown() {

  }

  @Test
  void run_withValidJsonResult_parsesScoreAndStatus() throws IOException {
    Path script = writeScript("print('{\"score\": 85.5, \"status\": \"SCORED\", "
        + "\"messages\": [\"validated ok\"]}')");

    SolverRunOutcome outcome = solverRunner.run(script, outputFile, null);

    assertThat(outcome.getResult().getScore()).isEqualByComparingTo(new BigDecimal("85.5"));
    assertThat(outcome.getResult().getStatus()).isEqualTo("SCORED");
    assertThat(outcome.getResult().getMessages()).containsExactly("validated ok");
  }

  @Test
  void run_withLogChatterBeforeJsonLine_stillParsesFinalLine() throws IOException {
    Path script =
        writeScript(
            "print('starting validation...')\n"
                + "print('checking constraints')\n"
                + "print('{\"score\": 10, \"status\": \"SCORED\", \"messages\": []}')");

    SolverRunOutcome outcome = solverRunner.run(script, outputFile, null);

    assertThat(outcome.getResult().getScore()).isEqualByComparingTo(BigDecimal.TEN);
    assertThat(outcome.getStdout()).contains("starting validation");
  }

  @Test
  void run_withNonZeroExitCode_throwsSolverExecutionException() throws IOException {
    Path script = writeScript("import sys\nsys.exit(1)");

    assertThatThrownBy(() -> solverRunner.run(script, outputFile, null))
        .isInstanceOf(SolverExecutionException.class)
        .hasFieldOrPropertyWithValue("errorType", "SOLVER_CRASH");
  }

  @Test
  void run_withMalformedFinalLine_throwsWithMalformedOutputType() throws IOException {
    Path script = writeScript("print('not json at all')");

    assertThatThrownBy(() -> solverRunner.run(script, outputFile, null))
        .isInstanceOf(SolverExecutionException.class)
        .hasFieldOrPropertyWithValue("errorType", "MALFORMED_OUTPUT");
  }

  @Test
  void run_withInvalidStatusValue_normalisesToFailed() throws IOException {
    Path script = writeScript("print('{\"score\": 0, \"status\": \"BANANA\"}')");

    SolverRunOutcome outcome = solverRunner.run(script, outputFile, null);

    assertThat(outcome.getResult().getStatus()).isEqualTo("FAILED");
    assertThat(outcome.getResult().getErrorType()).isEqualTo("MALFORMED_OUTPUT");
  }

  
}