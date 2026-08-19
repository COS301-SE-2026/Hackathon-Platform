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
    setField(config, "allowedEnvKeysRaw", "PATH,HOME,LANG,LC_ALL,SystemRoot");
    setField(config, "memoryLimitMb", 512);
    setField(config, "cpuLimitSeconds", 30);

    solverRunner = new SolverRunner(config);

    outputFile = tempDir.resolve("output.txt");
    Files.writeString(outputFile, "42\n");
  }

  @AfterEach
  void tearDown() {}

  @Test
  void run_withValidJsonResult_parsesScoreAndStatus() throws IOException {
    Path script =
        writeScript(
            "print('{\"score\": 85.5, \"status\": \"SCORED\", "
                + "\"messages\": [\"validated ok\"]}')");

    SolverRunOutcome outcome = solverRunner.run(script, outputFile, null, 1L);

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

    SolverRunOutcome outcome = solverRunner.run(script, outputFile, null, 1L);

    assertThat(outcome.getResult().getScore()).isEqualByComparingTo(BigDecimal.TEN);
    assertThat(outcome.getStdout()).contains("starting validation");
  }

  @Test
  void run_withNonZeroExitCode_throwsSolverExecutionException() throws IOException {
    Path script = writeScript("import sys\nsys.exit(1)");

    assertThatThrownBy(() -> solverRunner.run(script, outputFile, null, 1L))
        .isInstanceOf(SolverExecutionException.class)
        .hasFieldOrPropertyWithValue("errorType", "SOLVER_CRASH");
  }

  @Test
  void run_withMalformedFinalLine_throwsWithMalformedOutputType() throws IOException {
    Path script = writeScript("print('not json at all')");

    assertThatThrownBy(() -> solverRunner.run(script, outputFile, null, 1L))
        .isInstanceOf(SolverExecutionException.class)
        .hasFieldOrPropertyWithValue("errorType", "MALFORMED_OUTPUT");
  }

  @Test
  void run_withInvalidStatusValue_normalisesToFailed() throws IOException {
    Path script = writeScript("print('{\"score\": 0, \"status\": \"BANANA\"}')");

    SolverRunOutcome outcome = solverRunner.run(script, outputFile, null, 1L);

    assertThat(outcome.getResult().getStatus()).isEqualTo("FAILED");
    assertThat(outcome.getResult().getErrorType()).isEqualTo("MALFORMED_OUTPUT");
  }

  @Test
  void run_thatExceedsTimeout_isKilledAndThrowsTimeout() throws IOException {
    SolverExecutionConfig fastTimeoutConfig = new SolverExecutionConfig();
    setField(fastTimeoutConfig, "timeoutSeconds", 1);
    setField(fastTimeoutConfig, "pythonExecutable", "python");
    setField(fastTimeoutConfig, "workDir", tempDir.resolve("scratch2").toString());
    setField(fastTimeoutConfig, "maxOutputBytes", 1024 * 1024);
    SolverRunner fastTimeoutRunner = new SolverRunner(fastTimeoutConfig);

    Path script = writeScript("import time\ntime.sleep(10)");

    assertThatThrownBy(() -> fastTimeoutRunner.run(script, outputFile, null, 1L))
        .isInstanceOf(SolverExecutionException.class)
        .hasFieldOrPropertyWithValue("errorType", "TIMEOUT");
  }

  @Test
  void run_passesOutputAndLevelInputPaths_asArgumentsToScript() throws IOException {
    Path levelInputs = Files.createDirectory(tempDir.resolve("level-inputs"));
    Path script =
        writeScript(
            "import sys, json\n"
                + "out = sys.argv[1]\n"
                + "inputs = sys.argv[2]\n"
                + "print(json.dumps({\"score\": 1, \"status\": \"SCORED\", \"messages\": [\"out=\" + out, \"inputs=\" + inputs]}))");

    SolverRunOutcome outcome = solverRunner.run(script, outputFile, levelInputs, 2L);

    assertThat(outcome.getResult().getMessages().get(0)).contains(outputFile.toString());
    assertThat(outcome.getResult().getMessages().get(1)).contains(levelInputs.toString());
  }

  @Test
  void run_passesLevelId_asThirdArgumentToScript() throws IOException {
    Path script =
        writeScript(
            "import sys, json\n"
                + "level_id = sys.argv[3]\n"
                + "print(json.dumps({\"score\": 1, \"status\": \"SCORED\", \"messages\": [\"levelId=\" + level_id]}))");

    SolverRunOutcome outcome = solverRunner.run(script, outputFile, null, 7L);

    assertThat(outcome.getResult().getMessages().get(0)).isEqualTo("levelId=7");
  }

  @Test
  void run_withNullLevelId_passesEmptyStringArgument() throws IOException {
    Path script =
        writeScript(
            "import sys, json\n"
                + "level_id = sys.argv[3]\n"
                + "print(json.dumps({\"score\": 1, \"status\": \"SCORED\", \"messages\": [\"levelId=[\" + level_id + \"]\"]}))");

    SolverRunOutcome outcome = solverRunner.run(script, outputFile, null, null);

    assertThat(outcome.getResult().getMessages().get(0)).isEqualTo("levelId=[]");
  }

  @Test
  void run_doesNotExposeUnallowedEnvironmentVariables_toSolverProcess() throws IOException{

    Path script =
        writeScript(
            "import os, json\n"
                + "allowed = {'PATH', 'HOME', 'LANG', 'LC_ALL', 'SystemRoot'}\n"
                + "allowed_upper = {a.upper() for a in allowed}\n"
                + "leaked = sorted(k for k in os.environ if k.upper() not in allowed_upper)\n"
                + "print(json.dumps({\"score\": 1, \"status\": \"SCORED\", "
                + "\"messages\": [\"leaked=\" + \",\".join(leaked)]}))");
    
    SolverRunOutcome outcome = solverRunner.run(script, outputFile, null, 1L);

    assertThat(outcome.getResult().getMessages().get(0)).isEqualTo("leaked=");
  }

  @Test
  void run_stillExposesAllowListedEnvironmentVariables_toSolverProcess() throws IOException{
    Path script = 
        writeScript(
            "import os, json\n"
                + "present = 'PATH' in os.environ\n"
                + "print(json.dumps({\"score\": 1, \"status\": \"SCORED\", \"messages\": [\"pathPresent=\" + str(present)]}))");
    
    SolverRunOutcome outcome = solverRunner.run(script, outputFile, null, 1L);

    assertThat(outcome.getResult().getMessages().get(0)).isEqualTo("pathPresent=True");

  }

  @Test
  void run_thatExceedsMemoryLimit_isKilledOnUnixOrSucceedsOnWindowsFallback() throws IOException{
    SolverExecutionConfig lowMemConfig = new SolverExecutionConfig();
    setField(lowMemConfig, "timeoutSeconds", 5);
    setField(lowMemConfig, "pythonExecutable", "python");
    setField(lowMemConfig, "workDir", tempDir.resolve("scratch3").toString());
    setField(lowMemConfig, "maxOutputBytes", 1024 * 1024);
    setField(lowMemConfig, "allowedEnvKeysRaw", "PATH,HOME,LANG,LC_ALL,SystemRoot");
    setField(lowMemConfig, "memoryLimitMb", 64);
    setField(lowMemConfig, "cpuLimitSeconds", 30);
    SolverRunner lowMemRunner = new SolverRunner(lowMemConfig);

    Path script =
        writeScript(
          "data = bytearray(500 * 1024 * 1024)\n"
                + "print('{\"score\": 1, \"status\": \"SCORED\", \"messages\": [\"allocated ok\"]}')");
    
    boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
    if (windows) {
      SolverRunOutcome outcome = lowMemRunner.run(script, outputFile, null, 1L);
      assertThat(outcome.getResult().getMessages().get(0)).isEqualTo("allocated ok");

    } else {
      assertThatThrownBy(() -> lowMemRunner.run(script, outputFile, null, 1L))
          .isInstanceOf(SolverExecutionException.class)
          .hasFieldOrPropertyWithValue("errorType", "SOLVER_CRASH");
    }

  }



  private Path writeScript(String body) throws IOException {
    Path script = Files.createTempFile(tempDir, "solver-", ".py");
    Files.writeString(script, body, StandardCharsets.UTF_8);
    return script;
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
