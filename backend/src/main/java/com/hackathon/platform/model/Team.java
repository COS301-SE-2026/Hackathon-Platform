package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Entity representing a team in a hackathon event. */
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

  /** Default constructor. */
  public Team() { }

  /** Constructs a new Team with the given name, creator and event. */
  public Team(String teamName, UUID createdByUserId, UUID eventId) {
    this.teamName = teamName;
    this.createdByUserId = createdByUserId;
    this.eventId = eventId;
  }

  /** Returns the team ID. */
  public UUID getTeamId() {
    return teamId;
  }

  /** Sets the team ID. */
  public void setTeamId(UUID teamId) {
    this.teamId = teamId;
  }

  /** Returns the team name. */
  public String getTeamName() {
    return teamName;
  }

  /** Sets the team name. */
  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }

  /** Returns the ID of the user who created the team. */
  public UUID getCreatedByUserId() {
    return createdByUserId;
  }

  /** Sets the ID of the user who created the team. */
  public void setCreatedByUserId(UUID createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  /** Returns the ID of the event this team belongs to. */
  public UUID getEventId() {
    return eventId;
  }

  /** Sets the ID of the event this team belongs to. */
  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  /** Returns the creation timestamp of the team. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Sets the creation timestamp of the team. */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /** Returns the team status (ACTIVE or INACTIVE). */
  public String getStatus() {
    return status;
  }

  /** Sets the team status. */
  public void setStatus(String status) {
    this.status = status;
  }
}
