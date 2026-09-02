package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "event_participants", schema = "public")
@Setter
@Getter
public class EventRegistration {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "registration_id", updatable = false, nullable = false)
  private UUID registrationId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @Column(name = "dietary_requirements", columnDefinition = "TEXT")
  private String dietaryReq;

  @Column(name = "allergies", columnDefinition = "TEXT")
  private String allergies;

  @PrePersist
  protected void onCreate() {
    if (registeredAt == null) {
      registeredAt = Instant.now();
    }
  }
}
