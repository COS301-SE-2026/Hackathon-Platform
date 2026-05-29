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

  // Default constructor
  public SolverVersion() {}

  // Constructor with required fields
  public SolverVersion(UUID eventId, UUID uploadedBy, String storageKey) {
    this.eventId = eventId;
    this.uploadedBy = uploadedBy;
    this.storageKey = storageKey;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }

  public void setUploadedAt(Instant uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public UUID getUploadedBy() {
    return uploadedBy;
  }

  public void setUploadedBy(UUID uploadedBy) {
    this.uploadedBy = uploadedBy;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public void setStorageKey(String storageKey) {
    this.storageKey = storageKey;
  }

  public Integer getVersionNumber() {
    return versionNumber;
  }

  public void setVersionNumber(Integer versionNumber) {
    this.versionNumber = versionNumber;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public void setFileSize(Long fileSize) {
    this.fileSize = fileSize;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
}