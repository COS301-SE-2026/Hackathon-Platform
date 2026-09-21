package com.hackathon.platform.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminResponse {
  private UUID userId;
  private String firstName;
  private String lastName;
  private String email;
  private String status;
}
