package com.hackathon.platform.scoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.config.SolverExecutionConfig;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Executes a solver script against a submission ouput. This is done within a temp directory. The
 * solver works by running the solver script against a participants output file.
 */
@Component
@RequiredArgsConstructor
public class SolverRunner {
  private static final Logger logger = LoggerFactory.getLogger(SolverRunner.class);

  private final SolverExecutionConfig solverConfig;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * The solver is ran against a single submission output
   *
   * @param solverPath this is the path to the solver .py file on the local disk
   * @param outputPath This is the path to the participants output file on the local disk
   * @param levelInput The directory that contains the relevent level's files
   * @param levelId The numeric id of the level being scored (per hackathon), passed through to the
   *     solver
   * @return The result from the grader, with raw process output for logging purposes
   */
  public SolverRunOutcome run(Path solverPath, Path outputPath, Path levelInput, Long levelId) {
    Path tempDir = null;
    try {
      Path workingDir = Path.of(solverConfig.getWorkDir());
      Files.createDirectories(workingDir);
      tempDir = Files.createTempDirectory(workingDir, "run-");

      List<String> pythonCommand = new ArrayList<>();
      pythonCommand.add(solverConfig.getPythonExecutable());
      pythonCommand.add(solverPath.toAbsolutePath().toString());
      pythonCommand.add(outputPath.toAbsolutePath().toString());
      pythonCommand.add(levelInput != null ? levelInput.toAbsolutePath().toString() : "");
      pythonCommand.add(levelId != null ? levelId.toString() : "");

      // Wrap in a shell so we can apply ulimits. Temp commented out and added pythonCommand
      // straight to command until we see deployment
      List<String> command = pythonCommand;
      // if (isUnixLike()){
      //   command = new ArrayList<>();
      //   command.add("/bin/sh");
      //   command.add("-c");
      //   command.add(
      //       "ulimit -v "
      //           + (solverConfig.getMemoryLimitMb() * 1024L)
      //           + " -t "
      //           + solverConfig.getCpuLimitSeconds()
      //           + "; exec \"$@\"");
      //   command.add("solver-run");
      //   command.addAll(pythonCommand);

      // } else {
      //   logger.warn(
      //     "Non-Unix OS detected - running solver without ulimit-based CPU/memory limits");
      //   command = pythonCommand;

      // }

      ProcessBuilder builder = new ProcessBuilder(command);
      builder.directory(tempDir.toFile());
      builder.redirectErrorStream(false);

      // Deny-by-default
      builder.environment().clear();
      for (String key : solverConfig.getAllowedEnvKeys()) {
        String value = System.getenv(key);
        if (value != null) {
          builder.environment().put(key, value);
        }
      }

      Process process = builder.start();

      StreamCapture output =
          StreamCapture.read(process.getInputStream(), solverConfig.getMaxOutputBytes());
      StreamCapture errors =
          StreamCapture.read(process.getErrorStream(), solverConfig.getMaxOutputBytes());

      boolean done = process.waitFor(solverConfig.getTimeoutSeconds(), TimeUnit.SECONDS);
      if (!done) {
        process.destroyForcibly();
        logger.warn(
            "Solver has timed out after {}s: {}", solverConfig.getTimeoutSeconds(), solverPath);
        throw new SolverExecutionException(
            "The solver has exceeded its time limit of "
                + solverConfig.getTimeoutSeconds()
                + "seconds",
            "TIMEOUT");
      }

      String outputText = output.getTextSoFar();
      String errorsText = errors.getTextSoFar();
      int exit = process.exitValue();

      if (exit != 0) {
        logger.warn("The solver has exited with a non-zero ({}): {}", exit, solverPath);
        throw new SolverExecutionException(
            "The solver process has exited with the code "
                + exit
                + (errorsText.isBlank() ? "" : (": " + truncate(errorsText, 500))),
            "SOLVER_CRASH");
      }

      SolverResult res = getResult(outputText);
      return new SolverRunOutcome(res, outputText, errorsText);

    } catch (IOException e) {
      throw new SolverExecutionException("The solver has failed to start", "SOLVER_CRASH", e);
    } catch (InterruptedException e) {
      throw new SolverExecutionException(
          "The solver was interrupted during execution", "SOLVER_CRASH", e);
    } finally {
      if (tempDir != null) {
        remove(tempDir);
      }
    }
  }

  private SolverResult getResult(String outputText) {
    String[] lines = outputText.strip().split("\\R");
    String lastLine = null;
    for (int i = lines.length - 1; i >= 0; i--) {
      if (!lines[i].isBlank()) {
        lastLine = lines[i].strip();
        break;
      }
    }

    if (lastLine == null) {
      throw new SolverExecutionException(
          "Could not get a result from the solver", "MALFORMED_OUTPUT");
    }

    try {
      JsonNode node = objectMapper.readTree(lastLine);
      BigDecimal score =
          node.hasNonNull("score") ? new BigDecimal(node.get("score").asText()) : BigDecimal.ZERO;
      String status = node.hasNonNull("status") ? node.get("status").asText() : "FAILED";
      String errorType = node.hasNonNull("errorType") ? node.get("errorType").asText() : null;

      List<String> messages = new ArrayList<>();
      if (node.has("messages") && node.get("messages").isArray()) {
        node.get("messages").forEach(m -> messages.add(m.asText()));
      }

      if (!"SCORED".equals(status) && !"FAILED".equals(status)) {
        status = "FAILED";
        if (errorType == null) {
          errorType = "MALFORMED_OUTPUT";
        }
      }

      return new SolverResult(score, status, errorType, messages);
    } catch (Exception e) {
      throw new SolverExecutionException(
          "The solver's final line of output was not valid JSON: " + truncate(lastLine, 300),
          "MALFORMED_OUTPUT",
          e);
    }
  }

  // private boolean isUnixLike() {
  //   String os = System.getProperty("os.name", "").toLowerCase();
  //   return !os.contains("win");
  // }

  private String truncate(String text, int maxLen) {
    return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
  }

  private void remove(Path dir) {
    try (var stream = Files.walk(dir)) {
      stream
          .sorted((a, b) -> b.getNameCount() - a.getNameCount())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (IOException ignore) {
                }
              });
    } catch (IOException e) {
      logger.warn("Failed to remove the temp directory for the scorer {}: {}", dir, e.getMessage());
    }
  }
}
