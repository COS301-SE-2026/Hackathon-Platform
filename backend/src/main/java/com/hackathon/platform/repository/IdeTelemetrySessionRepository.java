package com.hackathon.platform.repository;

import com.hackathon.platform.model.IdeTelemetrySession;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeTelemetrySessionRepository extends JpaRepository<IdeTelemetrySession, UUID> {
  List<IdeTelemetrySession> findByWorkspaceIdAndUserIdAndEndedAtIsNull(
      UUID workspaceId, UUID userId);
}
