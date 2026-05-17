package com.hackathon.platform.dto;

import java.util.UUID;

public class EventStatusResponse {
  private UUID eventId;
  private String status;
  private String visibility;

  public EventStatusResponse(UUID eventId, String status, String visibility) {
    this.eventId = eventId;
    this.status = status;
    this.visibility = visibility;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }
}
