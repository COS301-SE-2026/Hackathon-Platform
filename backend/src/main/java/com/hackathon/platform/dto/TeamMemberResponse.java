package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;

public class TeamMemberResponse {
  private UUID userId;
  private String fullName;
  private String email;
  private Instant joinedAt;
  private String role; // "LEADER" or "MEMBER"

  public TeamMemberResponse() {}

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }

  public void setJoinedAt(Instant joinedAt) {
    this.joinedAt = joinedAt;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
