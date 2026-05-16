package com.hackathon.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** DTO for team creation request payload. */
public class CreateTeamRequest {

  @NotBlank(message = "Team name is required")
  private String teamName;

  @NotNull(message = "Event ID is required")
  private UUID eventId;

  /** Default constructor. */
  public CreateTeamRequest() { }

  /** Constructor with required fields. */
  public CreateTeamRequest(String teamName, UUID eventId) {
    this.teamName = teamName;
    this.eventId = eventId;
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
}
