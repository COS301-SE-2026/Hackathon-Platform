package com.hackathon.platform.repository;

import com.hackathon.platform.model.IdeTelemetryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface IdeTelemetryEventRepository extends JpaRepository<IdeTelemetryEvent, UUID> {
    List<IdeTelemetryEvent> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}