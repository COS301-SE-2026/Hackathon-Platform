package com.hackathon.platform.repository;

import com.hackathon.platform.model.IdeTelemetryEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeTelemetryEventRepository extends JpaRepository<IdeTelemetryEvent, UUID> {
  List<IdeTelemetryEvent> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
