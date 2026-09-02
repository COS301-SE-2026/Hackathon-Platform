package com.hackathon.platform.controller;

import com.hackathon.platform.dto.CreateForumCommentRequest;
import com.hackathon.platform.dto.CreateForumPostRequest;
import com.hackathon.platform.dto.ForumCommentResponse;
import com.hackathon.platform.dto.ForumPermissionResponse;
import com.hackathon.platform.dto.ForumPostDetailResponse;
import com.hackathon.platform.dto.ForumPostSummaryResponse;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.ForumAccessService;
import com.hackathon.platform.service.ForumService;
import com.hackathon.platform.service.ForumUpdateService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events/{eventId}/forum")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT', 'SUPERADMIN')")
public class ForumController {
  private final ForumService forumService;
  private final ForumAccessService forumAccessService;
  private final ForumUpdateService forumUpdateService;

  @GetMapping("/posts")
  public ResponseEntity<List<ForumPostSummaryResponse>> getPosts(
      @PathVariable UUID eventId, @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(forumService.getPosts(eventId, user));
  }

  @GetMapping("/posts/{postId}")
  public ResponseEntity<ForumPostDetailResponse> getPost(
      @PathVariable UUID eventId, @PathVariable UUID postId, @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(forumService.getPost(eventId, postId, user));
  }

  @PostMapping("/posts")
  public ResponseEntity<ForumPostDetailResponse> createPost(
      @PathVariable UUID eventId,
      @AuthenticationPrincipal User user,
      @Valid @RequestBody CreateForumPostRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(forumService.createPost(eventId, user, req));
  }

  @PostMapping("/posts/{postId}/comments")
  public ResponseEntity<ForumCommentResponse> createComment(
      @PathVariable UUID eventId,
      @PathVariable UUID postId,
      @AuthenticationPrincipal User user,
      @Valid @RequestBody CreateForumCommentRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(forumService.createComment(eventId, postId, user, req));
  }

  @GetMapping("/permissions")
  public ResponseEntity<ForumPermissionResponse> getPermissions(
      @PathVariable UUID eventId, @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(forumAccessService.getPermissions(eventId, user));
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("permitAll()")
  public SseEmitter streamForum(@PathVariable UUID eventId) {
    return forumUpdateService.subscribe(eventId);
  }

  @DeleteMapping("/posts/{postId}")
  public ResponseEntity<Void> deletePost(
      @PathVariable UUID eventId, @PathVariable UUID postId, @AuthenticationPrincipal User user) {
    forumService.deletePost(eventId, postId, user);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable UUID eventId,
      @PathVariable UUID commentId,
      @AuthenticationPrincipal User user) {
    forumService.deleteComment(eventId, commentId, user);
    return ResponseEntity.noContent().build();
  }
}
