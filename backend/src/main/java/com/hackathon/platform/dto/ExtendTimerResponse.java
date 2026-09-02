package com.hackathon.platform.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExtendTimerResponse {
  private UUID eventId;
  private int duration;
  private OffsetDateTime endDateTime;

  public ExtendTimerResponse() {}

  public ExtendTimerResponse(UUID eventId, int duration, OffsetDateTime endDateTime) {
    this.eventId = eventId;
    this.duration = duration;
    this.endDateTime = endDateTime;
  }
}
