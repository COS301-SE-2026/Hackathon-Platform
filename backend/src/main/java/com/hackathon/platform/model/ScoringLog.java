package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Metadata record for a team's scoring log file in blob storage.
 */
@Entity
@Table(name = "scoringlogs", schema = "public")
public class ScoringLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "team_id", nullable = false)
  private UUID teamId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "level_id", nullable = false)
  private Long levelId;

  @Column(name = "storage_key", nullable = false)
  private String storageKey;

  @Column(name = "submission_count", nullable = false)
  private int submissionCount = 0;

  @Column(name = "last_updated_at", nullable = false)
  private Instant lastUpdatedAt = Instant.now();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
  

  
}
