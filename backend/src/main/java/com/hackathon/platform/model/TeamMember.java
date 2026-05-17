package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Entity representing the link between a user and a team (membership or join request). */
@Entity
@Table(name = "teammembers")
public class TeamMember {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "team_member_id", updatable = false, nullable = false)
  private UUID teamMemberId;

  @Column(name = "team_id", nullable = false)
  private UUID teamId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "joined_at", nullable = false)
  private Instant joinedAt = Instant.now();

  @Column(name = "status", nullable = false, length = 20)
  private String status = "PENDING";

  /** Default constructor. */
  public TeamMember() { }

  /** Constructs a new TeamMember with the given team and user IDs. */
  public TeamMember(UUID teamId, UUID userId) {
    this.teamId = teamId;
    this.userId = userId;
  }

  /** Returns the unique ID of this team membership record. */
  public UUID getTeamMemberId() {
    return teamMemberId;
  }

  /** Sets the unique ID of this team membership record. */
  public void setTeamMemberId(UUID teamMemberId) {
    this.teamMemberId = teamMemberId;
  }

  /** Returns the ID of the team. */
  public UUID getTeamId() {
    return teamId;
  }

  /** Sets the ID of the team. */
  public void setTeamId(UUID teamId) {
    this.teamId = teamId;
  }

  /** Returns the ID of the user. */
  public UUID getUserId() {
    return userId;
  }

  /** Sets the ID of the user. */
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  /** Returns the date and time when the user joined (or requested to join). */
  public Instant getJoinedAt() {
    return joinedAt;
  }

  /** Sets the date and time when the user joined. */
  public void setJoinedAt(Instant joinedAt) {
    this.joinedAt = joinedAt;
  }

  /** Returns the membership status (PENDING, APPROVED, REJECTED, LEFT, REMOVED). */
  public String getStatus() {
    return status;
  }

  /** Sets the membership status. */
  public void setStatus(String status) {
    this.status = status;
  }
}
