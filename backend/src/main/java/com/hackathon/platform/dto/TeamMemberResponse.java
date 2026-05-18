package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;

/** DTO for team member information (used in view members response). */
public class TeamMemberResponse {
  private UUID userId;
  private String fullName;
  private String email;
  private Instant joinedAt;
  private String role; // "LEADER" or "MEMBER"

  /** Default constructor. */
  public TeamMemberResponse() {}

  /** Returns the user ID. */
  public UUID getUserId() {
    return userId;
  }

  /** Sets the user ID. */
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  /** Returns the full name of the member. */
  public String getFullName() {
    return fullName;
  }

  /** Sets the full name. */
  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  /** Returns the email address. */
  public String getEmail() {
    return email;
  }

  /** Sets the email address. */
  public void setEmail(String email) {
    this.email = email;
  }

  /** Returns the date when the member joined. */
  public Instant getJoinedAt() {
    return joinedAt;
  }

  /** Sets the join date. */
  public void setJoinedAt(Instant joinedAt) {
    this.joinedAt = joinedAt;
  }

  /** Returns the team role (LEADER or MEMBER). */
  public String getRole() {
    return role;
  }

  /** Sets the team role. */
  public void setRole(String role) {
    this.role = role;
  }
}
