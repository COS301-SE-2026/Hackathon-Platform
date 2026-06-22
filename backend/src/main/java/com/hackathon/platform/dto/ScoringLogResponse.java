package com.hackathon.platform.dto;

import java.time.Instant;

/** Response shape for a single scoring log line/entry. */
public class ScoringLogResponse {

  private Long id;
  private String logText;
  private String errorType;
  private Instant createdAt;

  public ScoringLogResponse() {}

  public ScoringLogResponse(Long id, String logText, String errorType, Instant createdAt) {
    this.id = id;
    this.logText = logText;
    this.errorType = errorType;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getLogText() {
    return logText;
  }

  public void setLogText(String logText) {
    this.logText = logText;
  }

  public String getErrorType() {
    return errorType;
  }

  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  
}
