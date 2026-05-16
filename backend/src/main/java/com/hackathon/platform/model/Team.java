package com.hackathon.platform.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teams")
public class Team {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "team_id", updatable = false, nullable = false)
  private UUID teamId;

  @Column(name = "team_name", nullable = false, length = 100)
  private String teamName;

  @Column(name = "created_by_user_id", nullable = false)
  private UUID createdByUserId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "status", nullable = false, length = 30)
  private String status = "ACTIVE";

  public Team() {}

  public Team(String teamName, UUID createdByUserId, UUID eventId) {
    this.teamName = teamName;
    this.createdByUserId = createdByUserId;
    this.eventId = eventId;
  }

  public UUID getTeamId() {
    return teamId;
  }

  public void setTeamId(UUID teamId) {
    this.teamId = teamId;
  }

  public String getTeamName() {
    return teamName;
  }

  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }

  public UUID getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(UUID createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
