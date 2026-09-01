package com.hackathon.platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response shape for a row in the admin "recent submissions" table.
 * 
 */
public class RecentSubmissionResponse {

  private Long submissionId;
  private UUID teamId;
  private String teamName;
  private UUID eventId;
  private String eventName;
  private short levelId;
  private short levelNumber;
  private String levelName;
  private BigDecimal score;
  private String status;
  private Instant submittedAt;

  public RecentSubmissionResponse() {}

  public RecentSubmissionResponse(
      Long submissionId,
      UUID teamId,
      String teamName,
      UUID eventId,
      String eventName,
      short levelId,
      short levelNumber,
      String levelName,
      BigDecimal score,
      String status,
      Instant submittedAt
    ) {
    this.submissionId = submissionId;
    this.teamId = teamId;
    this.teamName = teamName;
    this.eventId = eventId;
    this.eventName = eventName;
    this.levelId = levelId;
    this.levelNumber = levelNumber;
    this.levelName = levelName;
    this.score = score;
    this.status = status;
    this.submittedAt = submittedAt;

  }

  
}