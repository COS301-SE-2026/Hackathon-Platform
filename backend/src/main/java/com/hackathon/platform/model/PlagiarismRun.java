package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tracks progress of an admin-triggered batch plagiarism check for an event. */
@Entity
@Table(name = "plagiarism_run", schema = "public")
public class PlagiarismRun {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;
  

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  /** Null means all levels of the event. */
  @Column(name = "level_id")
  private Short levelId;

  @Column(name = "top_n", nullable = false)
  private int topN;

  @Column(name = "status", nullable = false, length = 20)
  private String status = "QUEUED";

  @Column(name = "pairs_compared", nullable = false)
  private int pairsCompared;

  @Column(name = "pairs_flagged", nullable = false)
  private int pairsFlagged;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "requested_by")
  private UUID requestedBy;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt = Instant.now();

  @Column(name = "completed_at")
  private Instant completedAt;

  
}