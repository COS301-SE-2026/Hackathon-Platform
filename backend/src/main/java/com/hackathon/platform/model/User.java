package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Entity representing a system user (participant or admin). */
@Entity
@Table(name = "users")
public class User {

  @Id
  @Column(name = "user_id", updatable = false, nullable = false)
  private UUID userId;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "email", nullable = false, length = 255, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "role_id", nullable = false)
  private Short roleId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "status", nullable = false, length = 30)
  private String status = "ACTIVE";

  /** Default constructor. */
  public User() {}

  /** Constructs a new User with the given basic information. */
  public User(
      UUID userId,
      String firstName,
      String lastName,
      String email,
      String passwordHash,
      Short roleId) {
    this.userId = userId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.passwordHash = passwordHash;
    this.roleId = roleId;
  }

  /** Returns the user ID. */
  public UUID getUserId() {
    return userId;
  }

  /** Sets the user ID. */
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  /** Returns the user's first name. */
  public String getFirstName() {
    return firstName;
  }

  /** Sets the user's first name. */
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  /** Returns the user's last name. */
  public String getLastName() {
    return lastName;
  }

  /** Sets the user's last name. */
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  /** Returns the user's email address. */
  public String getEmail() {
    return email;
  }

  /** Sets the user's email address. */
  public void setEmail(String email) {
    this.email = email;
  }

  /** Returns the hashed password. */
  public String getPasswordHash() {
    return passwordHash;
  }

  /** Sets the hashed password. */
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /** Returns the role ID (reference to roles table). */
  public Short getRoleId() {
    return roleId;
  }

  /** Sets the role ID. */
  public void setRoleId(Short roleId) {
    this.roleId = roleId;
  }

  /** Returns the creation timestamp. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Sets the creation timestamp. */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /** Returns the account status (ACTIVE, INACTIVE, SUSPENDED). */
  public String getStatus() {
    return status;
  }

  /** Sets the account status. */
  public void setStatus(String status) {
    this.status = status;
  }
}
