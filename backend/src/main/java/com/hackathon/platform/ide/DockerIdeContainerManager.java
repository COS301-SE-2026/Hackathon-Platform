package com.hackathon.platform.ide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DockerIdeContainerManager implements IdeContainerManager {
  private static final String IDE_IMAGE = "hackathon-browser-ide:1";

  @Override
  public IdeContainerSession startOrReuse(IdeWorkspaceResources resources) {
    Objects.requireNonNull(resources, "resources are required");
    ensureVolumeExists(resources.codeVolumeName());

    if (!containerExists(resources.containerName())) {
      createContainer(resources);
    }

    if (!containerRunning(resources.containerName())) {
      runDockerCommand("start", resources.containerName());
    }

    String port = getPublishedPort(resources.containerName());

    return new IdeContainerSession(resources.workspaceId(), Integer.parseInt(port), "RUNNING");
  }

  @Override
  public void stop(IdeWorkspaceResources resources) {
    runDockerCommand("stop", resources.containerName());
  }

  @Override
  public void removeContainer(IdeWorkspaceResources resources) {
    runDockerCommand("rm", "-f", resources.containerName());
  }

  @Override
  public void removeVolumes(IdeWorkspaceResources resources) {
    runDockerCommand("volume", "rm", resources.codeVolumeName());
  }

  private void ensureVolumeExists(String volumeName) {
    if (volumeExists(volumeName)) {
      return;
    }

    runDockerCommand("volume", "create", volumeName);
  }

  private boolean volumeExists(String volumeName) {
    return dockerCommandSucceeds("volume", "inspect", volumeName);
  }

  private boolean containerExists(String containerName) {
    return dockerCommandSucceeds("container", "inspect", containerName);
  }

  private boolean containerRunning(String containerName) {
    String output = runDockerCommand("inspect", "-f", "{{.State.Running}}", containerName);
    return Boolean.parseBoolean(output);
  }

  private void createContainer(IdeWorkspaceResources resources) {
    runDockerCommand(
        "create",
        "--name",
        resources.containerName(),
        "--memory",
        "768m",
        "--memory-swap",
        "768m",
        "--cpus",
        "1",
        "--pids-limit",
        "256",
        "--security-opt",
        "no-new-privileges:true",
        "--cap-drop",
        "ALL",
        "-p",
        "127.0.0.1::8080",
        "-v",
        resources.codeVolumeName() + ":/workspace",
        IDE_IMAGE);
  }

  private String getPublishedPort(String containerName) {
    String output = runDockerCommand("port", containerName, "8080/tcp");

    int colonIndex = output.lastIndexOf(':');

    if (colonIndex < 0 || colonIndex == output.length() - 1) {
      throw new IllegalStateException("IDE port could not be determined: " + output);
    }

    return output.substring(colonIndex + 1);
  }

  private boolean dockerCommandSucceeds(String... arguments) {
    String[] command = buildDockerCommand(arguments);

    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      process.getInputStream().readAllBytes();
      return process.waitFor() == 0;
    } catch (IOException e) {
      throw new IllegalStateException("Docker communication failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Docker command was interrupted", e);
    }
  }

  private String runDockerCommand(String... arguments) {
    String[] command = buildDockerCommand(arguments);

    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      int exitCode = process.waitFor();

      if (exitCode != 0) {
        throw new IllegalStateException("Docker command failed: " + output);
      }

      return output.trim();
    } catch (IOException e) {
      throw new IllegalStateException("Docker communication failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Docker command was interrupted", e);
    }
  }

  private String[] buildDockerCommand(String... arguments) {
    String[] command = new String[arguments.length + 1];
    command[0] = "docker";

    System.arraycopy(arguments, 0, command, 1, arguments.length);
    return command;
  }
}
