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
public class ForumCommentResponse {
    private UUID commentId;
    private UUID postId;
    private ForumAuthorResponse author;
    private String body;
    private Instant createdAt;
}