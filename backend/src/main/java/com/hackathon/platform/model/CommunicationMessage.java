package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "communication_messages")
@Getter
@Setter
@NoArgsConstructor
public class CommunicationMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "message_id", updatable = false, nullable = false)
  private UUID messageId;

  @Column(name = "channel_id", nullable = false)
  private UUID channelId;

  @Column(name = "created_by_user_id", nullable = false)
  private UUID createdByUserId;

  @Column(name = "title", length = 150)
  private String title;

  @Column(name = "body", nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(name = "severity", nullable = false, length = 20)
  private String severity = "INFO";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
