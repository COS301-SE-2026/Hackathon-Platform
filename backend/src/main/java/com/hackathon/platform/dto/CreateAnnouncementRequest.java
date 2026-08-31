package com.hackathon.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAnnouncementRequest {
  @NotBlank(message = "Announcement title must be provided")
  @Size(max = 150, message = "Announcement title cannot be longer than 150 characters")
  private String title;

  @NotBlank(message = "Announcement body cannot be empty")
  private String body;

  @Pattern(
      regexp = "INFO|IMPORTANT|URGENT",
      message = "Severity must be one of the following: INFO, IMPORTANT or URGENT")
  private String severity = "INFO";
}
