package com.hackathon.platform.ide;

import com.hackathon.platform.dto.WorkspaceFileEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EfsWorkspaceFileStore implements WorkspaceFileStore {
    private static final int MAX_SIZE = 1_000_000;
    private static final Path WORKSPACE_ROOT = Path.of("/workspace").toAbsolutePath().normalize();

    @Override
    public List<WorkspaceFileEntry> listFiles(IdeWorkspaceResources resources) {
        Path root = workspaceRoot(resources);
        if (!Files.exists(root)) {
            return List.of();
        }

        try (var stream = Files.walk(root, 20)) {
            List<WorkspaceFileEntry> files = new ArrayList<>();
            stream.filter(path -> !path.equals(root)).filter(path -> !isInternalRunPath(root, path))
                    .forEach(
                            path -> {
                                Path relative = root.relativize(path);
                                String normalized = relative.toString().replace('\\', '/');
                                if (Files.isRegularFile(path) || Files.isDirectory(path)) {
                                    files.add(new WorkspaceFileEntry(getFileName(normalized), normalized, Files.isDirectory(path)));
                                }
                            });

            files.sort(Comparator.comparing(WorkspaceFileEntry::directory).reversed().thenComparing(WorkspaceFileEntry::path));
            return files;
        } catch (IOException e) {
            throw new IllegalStateException("Workspace files clouldnt be listed", e);
        }
    }

    @Override
    public String readFile(IdeWorkspaceResources resources, String path) {
        Path file = resolveFile(resources, path);
        try {
            byte[] content = Files.readAllBytes(file);
            if (content.length > MAX_SIZE) {
                throw new IllegalArgumentException("File is too large to edit");
            }
            return new String(content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Workspace file could not be read", e);
        }
    }

    @Override
    public void writeFile(IdeWorkspaceResources resources, String path, String content) {
        if (content == null) {
            throw new IllegalArgumentException("File content is required");
        }

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_SIZE) {
            throw new IllegalArgumentException("File is too large to be saved");
        }

        Path file = resolveFile(resources, path);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Workspace file could not be written", e);
        }
    }

    private Path workspaceRoot(IdeWorkspaceResources resources) {
        if (resources == null) {
            throw new IllegalArgumentException("Workspace resources are required");
        }
        UUID workspaceId = resources.workspaceId();
        Path root = WORKSPACE_ROOT.resolve(workspaceId.toString()).normalize();
        if (!root.startsWith(WORKSPACE_ROOT)) {
            throw new IllegalArgumentException("Invalid workspace ID");
        }
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Workspace directory could not be created", e);
        }
        return root;
    }

    private Path resolveFile(IdeWorkspaceResources resources, String path) {
        String safePath = validatePath(path);
        Path root = workspaceRoot(resources);
        Path resolved = root.resolve(safePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid workspace file path");
        }
        return resolved;
    }

    private String validatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("File path required");
        }

        String norm = path.replace('\\', '/');
        if (norm.startsWith("/") || norm.length() > 500 || norm.contains("\0") || norm.contains("\n") || norm.contains("\r") || norm.contains("\t")) {
            throw new IllegalArgumentException("Invalid workspace file path");
        }

        for (String part : norm.split("/")) {
            if (part.isBlank() || part.equals(".") || part.equals("..")) {
                throw new IllegalArgumentException("Invalid workspace file path");
            }
        }
        return norm;
    }

    private boolean isInternalRunPath(Path root, Path path) {
        Path relative = root.relativize(path);
        return relative.getNameCount() > 0 && relative.getName(0).toString().equals(".hackathon-runs");
    }

    private String getFileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }
}
