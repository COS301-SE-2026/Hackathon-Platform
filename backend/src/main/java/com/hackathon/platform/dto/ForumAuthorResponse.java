package com.hackathon.platform.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForumAuthorResponse {
  private UUID userId;
  private String firstName;
  private String lastName;
  private String role;
}
