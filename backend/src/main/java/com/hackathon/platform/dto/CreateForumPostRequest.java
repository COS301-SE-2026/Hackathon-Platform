package com.hackathon.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateForumPostRequest {
  @NotBlank(message = "The message requires a title")
  @Size(max = 255, message = "The title is capped at 255 characters")
  private String title;

  @NotBlank(message = "The body cannot be empty")
  private String body;
}
