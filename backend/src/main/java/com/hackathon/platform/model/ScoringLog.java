package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scoringlogs", schema = "public")
public class ScoringLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "submission_id", nullable = false, unique = true)
  private Long submissionId;

  @Column(name = "team_id", nullable = false)
  private UUID teamId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "level_id", nullable = false)
  private Long levelId;

  @Column(name = "storage_key", nullable = false)
  private String storageKey;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public ScoringLog() {}

  public ScoringLog(Long submissionId, UUID teamId, UUID eventId, Long levelId, String storageKey) {
    this.submissionId = submissionId;
    this.teamId = teamId;
    this.eventId = eventId;
    this.levelId = levelId;
    this.storageKey = storageKey;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getSubmissionId() {
    return submissionId;
  }

  public void setSubmissionId(Long submissionId) {
    this.submissionId = submissionId;
  }

  public UUID getTeamId() {
    return teamId;
  }

  public void setTeamId(UUID teamId) {
    this.teamId = teamId;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public Long getLevelId() {
    return levelId;
  }

  public void setLevelId(Long levelId) {
    this.levelId = levelId;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public void setStorageKey(String storageKey) {
    this.storageKey = storageKey;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
