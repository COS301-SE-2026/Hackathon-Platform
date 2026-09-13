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

/**
 * Result of comparing two submissions for the same level during a plagiarism run.
 */
@Entity
@Table(name = "submission_similarity", schema = "public")
public class SubmissionSimilarity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "level_id", nullable = false)
  private short levelId;

  @Column(name = "submission_id_a", nullable = false)
  private Long submissionIdA;

  @Column(name = "submission_id_b", nullable = false)
  private Long submissionIdB;

  @Column(name = "team_id_a", nullable = false)
  private UUID teamIdA;

  @Column(name = "team_id_b", nullable = false)
  private UUID teamIdB;

  @Column(name = "structural_score", nullable = false, precision = 5, scale = 4)
  private BigDecimal structuralScore;

  @Column(name = "embedding_score", precision = 5, scale = 4)
  private BigDecimal embeddingScore;

  @Column(name = "combined_score", nullable = false, precision = 5, scale = 4)
  private BigDecimal combinedScore;

  @Column(name = "matched_kgram_count", nullable = false)
  private int matchedKgramCount;

  @Column(name = "flagged", nullable = false)
  private boolean flagged;

  @Column(name = "run_at", nullable = false)
  private Instant runAt = Instant.now();

  public SubmissionSimilarity() {}

  public SubmissionSimilarity(
    UUID eventId,
    short levelId,
    Long submissionIdA,
    Long submissionIdB,
    UUID teamIdA,
    UUID teamIdB,
    BigDecimal structuralScore,
    BigDecimal embeddingScore,
    BigDecimal combinedScore,
    int matchedKgramCount,
    boolean flagged
  ) {
    this.eventId = eventId;
    this.levelId = levelId;
    this.submissionIdA = submissionIdA;
    this.submissionIdB = submissionIdB;
    this.teamIdA = teamIdA;
    this.teamIdB = teamIdB;
    this.structuralScore = structuralScore;
    this.embeddingScore = embeddingScore;
    this.combinedScore = combinedScore;
    this.matchedKgramCount = matchedKgramCount;
    this.flagged = flagged;

  }

  public Long getId() {
    return id;
  }

  public UUID getEventId() {
    return eventId;
  }

  public short getLevelId() {

    return levelId;
  }

  public Long getSubmissionIdA() {
    return submissionIdA;
  }

  public Long getSubmissionIdB() {
    return submissionIdB;

  }

  public UUID getTeamIdA() {
    return teamIdA;
  }

  public UUID getTeamIdB() {
    return teamIdB;

  }

  public BigDecimal getStructuralScore() {
    return structuralScore;
  }

  public BigDecimal getEmbeddingScore() {
    return embeddingScore;

  }

  public BigDecimal getCombinedScore() {
    return combinedScore;
  }

  public int getMatchedKgramCount() {
    return matchedKgramCount;
  }

  public boolean isFlagged() {
    return flagged;
  }

  public Instant getRunAt() {
    return runAt;

  }


}