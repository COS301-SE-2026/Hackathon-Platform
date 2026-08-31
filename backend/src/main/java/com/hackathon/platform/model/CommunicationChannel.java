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
@Table(name = "communication_channels")
@Getter
@Setter
@NoArgsConstructor
public class CommunicationChannel {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "channel_id", updatable = false, nullable = false)
  private UUID channelId;

  @Column(name = "channel_type", nullable = false, length = 40)
  private String channelType;

  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "team_id")
  private UUID teamId;

  @Column(name = "direct_key", length = 100)
  private String directKey;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
