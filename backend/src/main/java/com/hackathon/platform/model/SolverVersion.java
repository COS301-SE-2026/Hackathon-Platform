// src/main/java/com/hackathon/platform/model/SolverVersion.java
package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solverversion", schema = "public")
public class SolverVersion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt = Instant.now();

  @Column(name = "uploaded_by", nullable = false)
  private UUID uploadedBy;

  @Column(name = "notes")
  private String notes;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "storage_key", nullable = false)
  private String storageKey;

  @Column(name = "version_number")
  private Integer versionNumber;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "content_type")
  private String contentType;

 
}