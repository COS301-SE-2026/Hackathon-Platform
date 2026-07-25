package com.hackathon.platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Response shape for a single submission, including its score, status and log metadata. */
public class SubmissionResponse {

  private Long submissionId;
  private UUID teamId;
  private short levelId;
  private Long solverVersionId;
  private BigDecimal score;
  private String status;
  private Instant submittedAt;
  private String outputFileName;
  private String sourceFileName;
  private ScoringLogResponse scoringLog;

  public SubmissionResponse() {}

  public SubmissionResponse(
      Long submissionId,
      UUID teamId,
      short levelId,
      Long solverVersionId,
      BigDecimal score,
      String status,
      Instant submittedAt,
      String outputFileName,
      String sourceFileName,
      ScoringLogResponse scoringLog) {

    this.submissionId = submissionId;
    this.teamId = teamId;
    this.levelId = levelId;
    this.solverVersionId = solverVersionId;
    this.score = score;
    this.status = status;
    this.submittedAt = submittedAt;
    this.outputFileName = outputFileName;
    this.sourceFileName = sourceFileName;
    this.scoringLog = scoringLog;
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

  public short getLevelId() {
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

  public ScoringLogResponse getScoringLog() {
    return scoringLog;
  }

  public void setScoringLog(ScoringLogResponse scoringLog) {
    this.scoringLog = scoringLog;
  }
}
