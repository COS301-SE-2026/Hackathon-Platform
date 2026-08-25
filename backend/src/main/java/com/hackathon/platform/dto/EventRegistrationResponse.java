package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventRegistrationResponse {
  private UUID regId;
  private UUID eventId;
  private Instant registeredAt;
  private String dietaryReq;
  private String allergies;

  public EventRegistrationResponse() {}

  public EventRegistrationResponse(UUID regId, UUID eventId, Instant registeredAt, String dietaryReq, String allergies) {
    this.regId = regId;
    this.eventId = eventId;
    this.registeredAt = registeredAt;
    this.dietaryReq = dietaryReq;
    this.allergies = allergies;
  }
}
