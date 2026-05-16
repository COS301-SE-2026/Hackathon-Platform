package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;

/** DTO for team creation/update responses. */
public class TeamResponse {

  private UUID teamId;
  private String teamName;
  private UUID eventId;
  private UUID createdByUserId;
  private Instant createdAt;
  private String status;

  /** Default constructor. */
  public TeamResponse() { }

  /** Constructor with all fields. */
  public TeamResponse(
      UUID teamId,
      String teamName,
      UUID eventId,
      UUID createdByUserId,
      Instant createdAt,
      String status) {
    this.teamId = teamId;
    this.teamName = teamName;
    this.eventId = eventId;
    this.createdByUserId = createdByUserId;
    this.createdAt = createdAt;
    this.status = status;
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

  /** Returns the event ID. */
  public UUID getEventId() {
    return eventId;
  }

  /** Sets the event ID. */
  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  /** Returns the ID of the user who created the team. */
  public UUID getCreatedByUserId() {
    return createdByUserId;
  }

  /** Sets the ID of the user who created the team. */
  public void setCreatedByUserId(UUID createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  /** Returns the creation timestamp. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Sets the creation timestamp. */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /** Returns the team status (ACTIVE/INACTIVE). */
  public String getStatus() {
    return status;
  }

  /** Sets the team status. */
  public void setStatus(String status) {
    this.status = status;
  }
}
