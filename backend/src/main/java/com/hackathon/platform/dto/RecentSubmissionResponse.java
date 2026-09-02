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

  public String getTeamName() {
    return teamName;

  }

  public void setTeamName(String teamName) {

    this.teamName = teamName;
  }

  public UUID getEventId() {

    return eventId;

  }

  public void setEventId(UUID eventId) {

    this.eventId = eventId;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public short getLevelId() {
    return levelId;
  }

  public void setLevelId(short levelId) {
    this.levelId = levelId;
  }

  public short getLevelNumber() {

    return levelNumber;
  }

  public void setLevelNumber(short levelNumber) {
    this.levelNumber = levelNumber;
  }

  public String getLevelName() {

    return levelName;
  }

  public void setLevelName(String levelName) {
    this.levelName = levelName;
  }

  public BigDecimal getScore() {
    return score;

  }

  public void setScore(BigDecimal score) {
    this.score = score;
  }

  public String getStatus() {

    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(Instant submittedAt) {
    this.submittedAt = submittedAt;
    
  }
}