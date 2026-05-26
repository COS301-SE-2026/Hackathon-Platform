// src/main/java/com/hackathon/platform/model/LevelFile.java
package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "levelfiles", schema = "public")
public class LevelFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "level_id", nullable = false)
  private Long levelId;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "storage_key", nullable = false)
  private String storageKey;

  @Column(name = "file_type", nullable = false, length = 20)
  private String fileType;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "content_type")
  private String contentType;


}