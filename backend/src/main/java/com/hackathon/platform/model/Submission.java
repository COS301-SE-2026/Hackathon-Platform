// src/main/java/com/hackathon/platform/model/Submission.java
package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submissions", schema = "public")
public class Submission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "team_id", nullable = false)
  private UUID teamId;

  @Column(name = "level_id", nullable = false)
  private Long levelId;

  @Column(name = "solver_version_id", nullable = false)
  private Long solverVersionId;

  @Column(name = "score", nullable = false, precision = 10, scale = 2)
  private BigDecimal score = BigDecimal.ZERO;

  @Column(name = "status", nullable = false, length = 20)
  private String status = "QUEUED";

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt = Instant.now();

  @Column(name = "source_code_storage_key", nullable = false)
  private String sourceCodeStorageKey;

  @Column(name = "output_storage_key", nullable = false)
  private String outputStorageKey;

  @Column(name = "output_file_name")
  private String outputFileName;

  @Column(name = "source_file_name")
  private String sourceFileName;

  @Column(name = "output_file_size")
  private Long outputFileSize;

  @Column(name = "source_file_size")
  private Long sourceFileSize;

  @Column(name = "output_content_type")
  private String outputContentType;

  @Column(name = "source_content_type")
  private String sourceContentType;

  // Default constructor
  public Submission() {}

  // Constructor with required fields
  public Submission(UUID teamId, Long levelId, Long solverVersionId, 
                    String sourceCodeStorageKey, String outputStorageKey) {
    this.teamId = teamId;
    this.levelId = levelId;
    this.solverVersionId = solverVersionId;
    this.sourceCodeStorageKey = sourceCodeStorageKey;
    this.outputStorageKey = outputStorageKey;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public UUID getTeamId() {
    return teamId;
  }

  public void setTeamId(UUID teamId) {
    this.teamId = teamId;
  }

  public Long getLevelId() {
    return levelId;
  }

  public void setLevelId(Long levelId) {
    this.levelId = levelId;
  }

  public Long getSolverVersionId() {
    return solverVersionId;
  }

  public void setSolverVersionId(Long solverVersionId) {
    this.solverVersionId = solverVersionId;
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

  public String getSourceCodeStorageKey() {
    return sourceCodeStorageKey;
  }

  public void setSourceCodeStorageKey(String sourceCodeStorageKey) {
    this.sourceCodeStorageKey = sourceCodeStorageKey;
  }

  public String getOutputStorageKey() {
    return outputStorageKey;
  }

  public void setOutputStorageKey(String outputStorageKey) {
    this.outputStorageKey = outputStorageKey;
  }

  public String getOutputFileName() {
    return outputFileName;
  }

  public void setOutputFileName(String outputFileName) {
    this.outputFileName = outputFileName;
  }

  public String getSourceFileName() {
    return sourceFileName;
  }

  public void setSourceFileName(String sourceFileName) {
    this.sourceFileName = sourceFileName;
  }

  public Long getOutputFileSize() {
    return outputFileSize;
  }

  public void setOutputFileSize(Long outputFileSize) {
    this.outputFileSize = outputFileSize;
  }

  public Long getSourceFileSize() {
    return sourceFileSize;
  }

  public void setSourceFileSize(Long sourceFileSize) {
    this.sourceFileSize = sourceFileSize;
  }

  public String getOutputContentType() {
    return outputContentType;
  }

  public void setOutputContentType(String outputContentType) {
    this.outputContentType = outputContentType;
  }

  public String getSourceContentType() {
    return sourceContentType;
  }

  public void setSourceContentType(String sourceContentType) {
    this.sourceContentType = sourceContentType;
  }
}