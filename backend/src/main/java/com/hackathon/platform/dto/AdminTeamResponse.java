package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AdminTeamResponse {


  private UUID teamId;
  private String teamName;
  private UUID eventId;
  private UUID createdByUserId;
  private Instant createdAt;
  private String status;
  private List<TeamMemberResponse> members;

  public AdminTeamResponse() {}

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

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public UUID getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(UUID createdByUserId) {
    this.createdByUserId = createdByUserId;
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

  public List<TeamMemberResponse> getMembers() {
   return members;
  }

  public void setMembers(List<TeamMemberResponse> members) {
    this.members = members;
  }
}