package com.hackathon.platform.dto;

import com.hackathon.platform.model.CodeWorkspace;
import java.time.Instant;
import java.util.UUID;

public record CodeWorkspaceResponse(UUID workspaceId, UUID eventId, UUID teamId, UUID hackathonId, short levelId, String language, Instant createdAt) {
    public static CodeWorkspaceResponse from(CodeWorkspace work) {
        return new CodeWorkspaceResponse(
            work.getWorkspaceId(),
            work.getEventId(),
            work.getTeamId(),
            work.getHackathonId(),
            work.getLevelId(),
            work.getLanguage(),
            work.getCreatedAt()
        );
    }
}