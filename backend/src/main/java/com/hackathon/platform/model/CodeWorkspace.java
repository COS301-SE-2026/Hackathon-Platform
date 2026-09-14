package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "code_workspaces", schema = "public")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodeWorkspace {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "team_id", nullable = false, updatable = false)
    private UUID teamId;

    @Column(name = "hackathon_id", nullable = false, updatable = false)
    private UUID hackathonId;

    @Column(name = "level_id", nullable = false, updatable = false)
    private short levelId;

    @Column(name = "language", nullable = false, updatable = false, length = 20)
    private String language = "JAVA";

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
        private Instant createdAt = Instant.now();

        public CodeWorkspace(UUID eventId, UUID teamId, UUID hackathonId, short levelId, UUID createdByUserId) {
            this.eventId = eventId;
            this.teamId = teamId;
            this.hackathonId = hackathonId;
            this.levelId = levelId;
            this.createdByUserId = createdByUserId;
        }

}