package com.hackathon.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateForumCommentRequest {
  @NotBlank(message = "The comment cannot be empty")
  private String body;
}
