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

  @Column(name = "hackathon_id", nullable = false)
  private UUID hackathonId;

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

  public ScoringLog() {}

  public ScoringLog(UUID teamId, UUID hackathonId, Long levelId, String storageKey) {
    this.teamId = teamId;
    this.hackathonId = hackathonId;
    this.levelId = levelId;
    this.storageKey = storageKey;
    
  }


  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public UUID getTeamId() { return teamId; }
  public void setTeamId(UUID teamId) { this.teamId = teamId; }

  public UUID getHackathonId() { return hackathonId; }
  public void setHackathonId(UUID hackathonId) { this.hackathonId = hackathonId; }

  public Long getLevelId() { return levelId; }
  public void setLevelId(Long levelId) { this.levelId = levelId; }

  public String getStorageKey() { return storageKey; }
  public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

  public int getSubmissionCount() { return submissionCount; }
  public void setSubmissionCount(int submissionCount) { this.submissionCount = submissionCount; }

  public Instant getLastUpdatedAt() { return lastUpdatedAt; }
  public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }


}
