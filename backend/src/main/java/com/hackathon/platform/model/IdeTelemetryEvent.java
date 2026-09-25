package com.hackathon.platform.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ide_telemetry_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdeTelemetryEvent {
  @Id
  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "session_id", nullable = false)
  private UUID sessionId;

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "event_type", nullable = false, length = 50)
  private String eventType;

  @Column(name = "client_timestamp", nullable = false)
  private LocalDateTime clientTimestamp;

  @Column(name = "received_at", nullable = false)
  private LocalDateTime receivedAt;

  @Column(name = "sequence_number", nullable = false)
  private long sequenceNumber;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private JsonNode payload;

  @PrePersist
  public void prePersist() {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }

    if (receivedAt == null) {
      receivedAt = LocalDateTime.now();
    }
  }
}
