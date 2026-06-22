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

  
}
