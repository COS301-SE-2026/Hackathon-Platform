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

 

  
}