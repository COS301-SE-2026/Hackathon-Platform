package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "certificate_generation_run")
@Getter
@Setter
public class CertificateGenerationRun {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "run_id", updatable = false, nullable = false)
  private UUID runId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "template_id", nullable = false)
  private UUID templateId;

  @Column(nullable = false, length = 20)
  private String scope;

  @Column(name = "top_n")
  private Integer topN;

  @Column(nullable = false, length = 20)
  private String status = "PENDING";

  @Column(name = "total_count", nullable = false)
  private int totalCount = 0;

  @Column(name = "completed_count", nullable = false)
  private int completedCount = 0;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "requested_by_user_id", nullable = false)
  private UUID requestedByUserId;

  @Column(name = "requested_at", nullable = false)
  private OffsetDateTime requestedAt = OffsetDateTime.now();

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;
}
