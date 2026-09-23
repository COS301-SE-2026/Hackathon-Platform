package com.hackathon.platform.repository;

import com.hackathon.platform.model.IdeTelemetrySession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface IdeTelemetrySessionRepository extends JpaRepository<IdeTelemetrySession, UUID> {
    List<IdeTelemetrySession> findByWorkspaceIdAndUserIdAndEndedAtIsNull(UUID workspaceId, UUID userId);
}