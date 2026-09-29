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
@Table(name = "certificate_issued")
@Getter
@Setter
public class CertificateIssued {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "certificate_id", updatable = false, nullable = false)
  private UUID certificateId;

  @Column(name = "run_id", nullable = false)
  private UUID runId;

  @Column(name = "template_id", nullable = false)
  private UUID templateId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "team_id")
  private UUID teamId;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "certificate_type", nullable = false, length = 20)
  private String certificateType;

  @Column(name = "recipient_name", nullable = false, length = 255)
  private String recipientName;

  @Column(name = "rank_at_issue")
  private Integer rankAtIssue;

  @Column(name = "storage_key", nullable = false, columnDefinition = "TEXT")
  private String storageKey;

  @Column(name = "verification_code", nullable = false, unique = true, length = 20)
  private String verificationCode;

  @Column(name = "issued_at", nullable = false)
  private OffsetDateTime issuedAt = OffsetDateTime.now();
}
