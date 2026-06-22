package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entity representing a single log entry produced while scoring a submission. Maps to the
 * existing 'scoringlogs' table. A submission can have many log entries.
 */
@Entity
@Table(name = "scoringlogs", schema = "public")
public class ScoringLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "submission_id", nullable = false)
  private Long submissionId;

  @Column(name = "log_text", nullable = false, columnDefinition = "TEXT")
  private String logText;

  @Column(name = "error_type", length = 50)
  private String errorType;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  // Default constructor
  public ScoringLog() {}

  // Constructor with required fields
  public ScoringLog(Long submissionId, String logText) {
    this.submissionId = submissionId;
    this.logText = logText;
  }

  public ScoringLog(Long submissionId, String logText, String errorType) {
    this.submissionId = submissionId;
    this.logText = logText;
    this.errorType = errorType;
  }

  // Getters and Setters
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
