package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;

/** Response shape for a single submission's scoring log. */
public class ScoringLogResponse {

  private Long submissionId;
  private UUID teamId;
  private UUID eventId;
  private String storageKey;
  private Instant createdAt;
  private String logContent;

  public ScoringLogResponse() {}

  public ScoringLogResponse(
      Long submissionId,
      UUID teamId,
      UUID eventId,
      String storageKey,
      Instant createdAt,
      String logContent) {
    this.submissionId = submissionId;
    this.teamId = teamId;
    this.eventId = eventId;
    this.storageKey = storageKey;
    this.createdAt = createdAt;
    this.logContent = logContent;
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

  public String getLogContent() {
    return logContent;
  }

  public void setLogContent(String logContent) {
    this.logContent = logContent;
  }
}
