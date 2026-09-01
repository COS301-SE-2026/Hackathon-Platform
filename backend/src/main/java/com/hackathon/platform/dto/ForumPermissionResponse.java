package com.hackathon.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForumPermissionResponse {
    private boolean canCreatePost;
    private boolean canComment;
    private boolean canModerate;
}