package com.hackathon.platform.ide;

import java.util.Objects;
import java.util.UUID;

public record IdeWorkspaceResources(UUID workspaceId, String containerName, String codeVolumeName) {
    public static IdeWorkspaceResources forWorkspace(UUID workspaceId) {
        Objects.requireNonNull(workspaceId, "an ID for a workspace is required");
        return new IdeWorkspaceResources(workspaceId, "hackathon-ide-" + workspaceId, "hackathon-code-" + workspaceId);
    }
}