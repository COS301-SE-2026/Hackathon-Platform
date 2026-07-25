package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;

/** Response shape for a team's scoring log. */
public class ScoringLogResponse {

  private UUID teamId;
  private UUID eventId;
  private String storageKey;
  private int submissionCount;
  private Instant lastUpdatedAt;
  private String logContent;

  public ScoringLogResponse() {}

  public ScoringLogResponse(
      UUID teamId,
      UUID eventId,
      String storageKey,
      int submissionCount,
      Instant lastUpdatedAt,
      String logContent) {
    this.teamId = teamId;
    this.eventId = eventId;
    this.storageKey = storageKey;
    this.submissionCount = submissionCount;
    this.lastUpdatedAt = lastUpdatedAt;
    this.logContent = logContent;
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

  public int getSubmissionCount() {
    return submissionCount;
  }

  public void setSubmissionCount(int submissionCount) {
    this.submissionCount = submissionCount;
  }

  public Instant getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void setLastUpdatedAt(Instant lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }

  public String getLogContent() {
    return logContent;
  }

  public void setLogContent(String logContent) {
    this.logContent = logContent;
  }
}
