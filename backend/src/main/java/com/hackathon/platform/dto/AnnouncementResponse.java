package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AnnouncementResponse {
  private UUID messageId;
  private UUID eventId;
  private String title;
  private String body;
  private String severity;
  private Instant createdAt;
}
