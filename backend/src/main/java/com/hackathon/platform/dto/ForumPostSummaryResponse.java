package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostSummaryResponse {
  private UUID postId;
  private String title;
  private ForumAuthorResponse author;
  private Instant createdAt;
  private long replyCount;
}
