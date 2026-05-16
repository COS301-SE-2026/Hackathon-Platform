package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Represents a user */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "first_name", nullable = false, length = 50)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private String status = "ACTIVE";

  @ManyToOne(fetch = FetchType.EAGER) // eager cause we need role for loading a user for auth
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  /** Returns the role as spring security. It requires ROLE_ before hasRole() checks */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.getName()));
  }

  /** Return bcrypt hash */
  @Override
  public String getPassword() {
    return passwordHash;
  }

  /** This uses email as the unique identifier. */
  @Override
  public String getUsername() {
    return email;
  }

  /** Account is never expired. */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /** Account is never locked we are using our own status field. */
  @Override
  public boolean isAccountNonLocked() {
    return "ACTIVE".equals(status);
  }

  /** Credentials will never expire */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /** User will be enabled if their status is active. */
  @Override
  public boolean isEnabled() {
    return "ACTIVE".equals(status);
  }
}
