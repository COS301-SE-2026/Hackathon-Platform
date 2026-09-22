package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ide_telemetry_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdeTelemetrySession {
    @Id
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    public void prePersist() {
        if (sessionId == null) {
            sessionId = UUID.randomUUID();
        }

        LocalDateTime now = LocalDateTime.now();

        if (startedAt == null) {
            startedAt = now;
        }

        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
    }
}