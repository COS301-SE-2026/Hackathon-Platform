package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response shape for a team's scoring log. Contains metadata about the log file plus
 * the full text content downloaded from blob storage.
 */
public class ScoringLogResponse {

  private UUID teamId;
  private UUID hackathonId;
  private String storageKey;
  private int submissionCount;
  private Instant lastUpdatedAt;
  private String logContent;   // full text of the log file from blob

  public ScoringLogResponse() {}

  public ScoringLogResponse(
      UUID teamId,
      UUID hackathonId,
      String storageKey,
      int submissionCount,
      Instant lastUpdatedAt,
      String logContent) {
    this.teamId = teamId;
    this.hackathonId = hackathonId;
    this.storageKey = storageKey;
    this.submissionCount = submissionCount;
    this.lastUpdatedAt = lastUpdatedAt;
    this.logContent = logContent;


  }
  

  public UUID getTeamId() { return teamId; }
  public void setTeamId(UUID teamId) { this.teamId = teamId; }

  public UUID getHackathonId() { return hackathonId; }
  public void setHackathonId(UUID hackathonId) { this.hackathonId = hackathonId; }

  public String getStorageKey() { return storageKey; }
  public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

  public int getSubmissionCount() { return submissionCount; }
  public void setSubmissionCount(int submissionCount) { this.submissionCount = submissionCount; }

  public Instant getLastUpdatedAt() { return lastUpdatedAt; }
  public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

  public String getLogContent() { return logContent; }
  public void setLogContent(String logContent) { this.logContent = logContent; }


}
