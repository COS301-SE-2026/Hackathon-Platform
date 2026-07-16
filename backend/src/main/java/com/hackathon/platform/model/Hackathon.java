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
@Table(name = "hackathon", schema = "public")
public class Hackathon {

  @Setter
  @Getter
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "hackathon_id", updatable = false, nullable = false)
  private UUID hackathonId;

  @Setter
  @Getter
  @Column(name = "name", nullable = false)
  private String name;

  @Setter
  @Getter
  @Column(name = "created_at", updatable = false, insertable = false)
  private OffsetDateTime createdAt;

  @Setter
  @Getter
  @Column(name = "description")
  private String description;
}
