package com.hackathon.platform.ide;

import com.hackathon.platform.dto.WorkspaceRunResponse;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class DockerJavaWorkspaceRunner implements WorkspaceCodeRunner {
    private static final String RUNNER_IMAGE = "hackathon-java-runner:1";
    private static final long TIMEOUT_SECONDS = 10;
    private static final int MAX_OUTPUT_BYTES = 64_000;
    private static final String RUN_COMMAND = 
    """
    set -e
    mkdir -p /tmp/build
    find /workspace -type f -name '*.java' -print0 | xargs -0 javac -d /tmp/build
    java -cp /tmp/build Main
    """;

    @Override
    public WorkspaceRunResponse run(IdeWorkspaceResources resources) {
        Objects.requireNonNull(resources, "Workspace resources are required");

        String runnerName = "hackathon-run-" + resources.workspaceId() + "-" +UUID.randomUUID();

        List<String> command = buildRunCommand(resources, runnerName);
        ExecutorService outputExecutor = Executors.newSingleThreadExecutor();
        Process process = null;

        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            Process runningProcess = process;
            Future<String> outputFuture = outputExecutor.submit(() -> readOutput(runningProcess.getInputStream()));
            boolean complete = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!complete) {
                removeRunnerContainer(runnerName);
                process.destroyForcibly();
                process.waitFor();

                String output = getOutput(outputFuture);
                String error = output.isBlank() ? "Execution timed out after " + TIMEOUT_SECONDS + "s": output + System.lineSeparator() + "Execution timed out after " + TIMEOUT_SECONDS + "s";

                return new WorkspaceRunResponse(false, 124, "", error);
            }

            int exitCode = process.exitValue();
            String output = getOutput(outputFuture);

            if (exitCode == 0) {
                return new WorkspaceRunResponse(true, exitCode, output, "");
            }

            return new WorkspaceRunResponse(false, exitCode, "", output);
        } catch (IOException e) {
            throw new IllegalStateException("Java executor could not communicate with Docker", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            removeRunnerContainer(runnerName);

            throw new IllegalStateException("Java execution was interrupted", e);
        } finally {
            outputExecutor.shutdownNow();
            removeRunnerContainer(runnerName);
        }
    }

    private List<String> buildRunCommand(IdeWorkspaceResources resources, String runnerName) {
        List<String> command = new ArrayList<>();

        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("--name");
        command.add(runnerName);
        command.add("--network");
        command.add("none");
        command.add("--memory");
        command.add("256m");
        command.add("--cpus");
        command.add("1.0");
        command.add("--pids-limit");
        command.add("100");
        command.add("--read-only");
        command.add("--security-opt");
        command.add("no-new-privileges");
        command.add("--cap-drop");
        command.add("ALL");
        command.add("--tmpfs");
        command.add("/tmp:rw,size=64m");
        command.add("-v");
        command.add(resources.codeVolumeName() + ":/workspace:ro");
        command.add("-w");
        command.add("/workspace");
        command.add(RUNNER_IMAGE);
        command.add("sh");
        command.add("-lc");
        command.add(RUN_COMMAND);

        return command;
    }

    private String readOutput(InputStream input) throws IOException {
        ByteArrayOutputStream stored = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int storedBytes = 0;
        boolean truncated = false;

        int read;

        while ((read = input.read(buffer)) != -1) {
            int remaining = MAX_OUTPUT_BYTES - storedBytes;

            if (remaining > 0) {
                int amount = Math.min(remaining, read);
                stored.write(buffer, 0, amount);
                storedBytes += amount;
            }

            if (storedBytes >= MAX_OUTPUT_BYTES) {
                truncated = true;
            }
        }

        String output = stored.toString(StandardCharsets.UTF_8);

        if (truncated) {
            output += System.lineSeparator() + "[Output truncated]";
        }

        return output.trim();
    }

    private String getOutput(Future<String> future) {
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "Execution output could not be recorded";
        }
    }

    private void removeRunnerContainer(String runnerName) {
        try {
            Process process = new ProcessBuilder("docker", "rm", "-f", runnerName).redirectErrorStream(true).start();
            process.getInputStream().readAllBytes();
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (IOException e) {
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
