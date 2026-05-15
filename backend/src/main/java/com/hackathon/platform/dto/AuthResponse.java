package com.hackathon.platform.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {

  private String token;
  private UUID userId;
  private String firstName;
  private String lastName;
  private String email;
  private String role;
}
