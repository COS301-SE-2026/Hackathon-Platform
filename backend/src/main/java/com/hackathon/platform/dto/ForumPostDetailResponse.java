package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostDetailResponse {
    private UUID postId;
    private String title;
    private String body;
    private ForumAuthorResponse author;
    private Instant createdAt;
    private List<ForumCommentResponse> comments;
}
