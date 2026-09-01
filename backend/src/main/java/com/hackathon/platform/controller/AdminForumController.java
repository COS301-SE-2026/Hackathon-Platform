package com.hackathon.platform.controller;

import com.hackathon.platform.model.User;
import com.hackathon.platform.service.ForumService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/events/{eventId}/forum")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")

public class AdminForumController {
    private final ForumService forumService;

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID eventId, @PathVariable UUID postId, @AuthenticationPrincipal User user) {
        forumService.deletePost(eventId, postId, user);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID eventId, @PathVariable UUID commentId, @AuthenticationPrincipal User user) {
        forumService.deleteComment(eventId, commentId, user);

        return ResponseEntity.noContent().build();
    }
}