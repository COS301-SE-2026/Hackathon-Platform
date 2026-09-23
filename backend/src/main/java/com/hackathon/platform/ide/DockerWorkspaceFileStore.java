package com.hackathon.platform.ide;

import com.hackathon.platform.dto.WorkspaceFileEntry;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class DockerWorkspaceFileStore implements WorkspaceFileStore {
    private static final int MAX_SIZE = 1_000_000;

    @Override
    public List<WorkspaceFileEntry> listFiles(IdeWorkspaceResources resources) {
        String output = runDockerCommand("exec", resources.containerName(), "find", "/workspace", "-mindepth", "1", "-maxdepth", "20", "-printf", "%y\t%P\n");

        if (output.isBlank()) {
            return List.of();
        }

        List<WorkspaceFileEntry> files = new ArrayList<>();

        for (String line : output.lines().toList()) {
            int separator = line.indexOf('\t');
            if (separator <= 0) {
                continue;
            }

            char type = line.charAt(0);

            if (type != 'f' && type != 'd') {
                continue;
            }

            String path = line.substring(separator + 1);
            String name = getFileName(path);
            files.add(new WorkspaceFileEntry(name, path, type == 'd'));
        }

        files.sort(Comparator.comparing(WorkspaceFileEntry::directory).reversed().thenComparing(WorkspaceFileEntry::path));

        return files;
    }

    @Override
    public String readFile(IdeWorkspaceResources resources, String path) {
        String safePath = validatePath(path);
        byte[] content = runDockerCommandBytes("exec", resources.containerName(), "cat", "--", "/workspace/" + safePath);

        if (content.length > MAX_SIZE) {
            throw new IllegalArgumentException("File is too large to edit");
        }

        return new String(content, StandardCharsets.UTF_8);
    }

    @Override
    public void writeFile(IdeWorkspaceResources resources, String path, String content) {
        String safePath = validatePath(path);
        if (content == null) {
            throw new IllegalArgumentException("File content is required");
        }

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        if (bytes.length > MAX_SIZE) {
            throw new IllegalArgumentException("File is to large to be saved");
        }

        int slash = safePath.lastIndexOf('/');

        if (slash >= 0) {
            String parent = safePath.substring(0, slash);

            runDockerCommand("exec", resources.containerName(), "mkdir", "-p", "--", "/workspace/" + parent);
        }

        writeDockerFile(resources.containerName(), "/workspace/" + safePath, bytes);
    }

    private String validatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("File path required");
        }

        String norm = path.replace('\\', '/');

        if (norm.startsWith("/") || norm.length() > 500 || norm.contains("\0") || norm.contains("\n") || norm.contains("\r") || norm.contains("\t")) {
            throw new IllegalArgumentException("Invalid workspace file path");
        }

        String[] parts = norm.split("/");

        for (String part : parts) {
            if (part.isBlank() || part.equals(".") || part.equals("..")) {
                throw new IllegalArgumentException("Invalid workspace file path");
            }
        }

        return norm;
    }

    private String getFileName(String path) {
        int slash = path.lastIndexOf('/');
        if (slash < 0) {
            return path;
        }

        return path.substring(slash + 1);
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
        } 
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Docker command was interrupted", e);
        }
    }

    private byte[] runDockerCommandBytes(String... arguments) {
        String[] command = buildDockerCommand(arguments);

        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            byte[] output = process.getInputStream().readAllBytes();
            byte[] error = process.getErrorStream().readAllBytes();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IllegalStateException("Docker command failed: " + new String(output, StandardCharsets.UTF_8));
            }

            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Docker communication failed", e);
        } 
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Docker command was interrupted", e);
        }
    }

    private void writeDockerFile(String containerName, String path, byte[] content) {
        String[] command = buildDockerCommand("exec", "-i", containerName, "tee", "--", path);

        try {
            Process process = new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();

            try (var input = process.getOutputStream()) {
                input.write(content);
            }

            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IllegalStateException("Workspace file could not be written: " + error);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Docker communication failed" + e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Docker command interrpted" + e);
        }
    }

    private String[] buildDockerCommand(String... arguments) {
        String[] command = new String[arguments.length + 1];
        command[0] = "docker";

        System.arraycopy(arguments, 0, command, 1, arguments.length);
        return command;
    }
}