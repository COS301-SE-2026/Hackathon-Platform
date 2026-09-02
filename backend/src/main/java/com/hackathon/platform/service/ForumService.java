package com.hackathon.platform.service;

import com.hackathon.platform.dto.CreateForumCommentRequest;
import com.hackathon.platform.dto.CreateForumPostRequest;
import com.hackathon.platform.dto.ForumAuthorResponse;
import com.hackathon.platform.dto.ForumCommentResponse;
import com.hackathon.platform.dto.ForumPostSummaryResponse;
import com.hackathon.platform.dto.ForumPostDetailResponse;
import com.hackathon.platform.model.ForumComment;
import com.hackathon.platform.model.ForumPost;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.ForumCommentRepository;
import com.hackathon.platform.repository.ForumPostRepository;
import com.hackathon.platform.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForumService {
    private final ForumPostRepository postRepo;
    private final ForumCommentRepository commentRepo;
    private final UserRepository userRepo;
    private final ForumAccessService accessService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ForumPostSummaryResponse> getPosts(UUID eventId, User user) {
        accessService.requireForumAccess(eventId, user);
        return postRepo.findByEventIdAndIsDeletedFalseOrderByCreatedAtDesc(eventId).stream().map(this::toPostSummary).toList();
    }

    @Transactional(readOnly = true)
    public ForumPostDetailResponse getPost(UUID eventId, UUID postId, User user) {
        accessService.requireForumAccess(eventId, user);
        ForumPost post = requirePost(eventId, postId);
        List<ForumCommentResponse> comments = commentRepo.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId).stream().map(this::toCommentResponse).toList();

        return new ForumPostDetailResponse(post.getPostId(), post.getTitle(), post.getBody(), getAuthor(post.getAuthorId()), post.getCreatedAt(), comments);
    }

    @Transactional
    public ForumPostDetailResponse createPost(UUID eventId, User user, CreateForumPostRequest req) {
        accessService.requireForumAccess(eventId, user);
        ForumPost post = new ForumPost(eventId, user.getUserId(), req.getTitle().trim(), req.getBody());
        ForumPost saved = postRepo.save(post);
        eventPublisher.publishEvent(new ForumUpdatedEvent(eventId, "POST_CREATED", saved.getPostId()));

        return new ForumPostDetailResponse(saved.getPostId(), saved.getTitle(), saved.getBody(), getAuthor(saved.getAuthorId()), saved.getCreatedAt(), List.of());
    }

    @Transactional
    public ForumCommentResponse createComment(UUID eventId, UUID postId, User user, CreateForumCommentRequest req) {
        accessService.requireForumAccess(eventId, user);
        ForumPost post = requirePost(eventId, postId);
        ForumComment comment = new ForumComment(post.getPostId(), user.getUserId(), req.getBody());
        ForumComment saved = commentRepo.save(comment);
        eventPublisher.publishEvent(new ForumUpdatedEvent(eventId, "COMMENT_CREATED", saved.getCommentId()));

        return toCommentResponse(saved);
    }

    @Transactional
    public void deletePost(UUID eventId, UUID postId, User user) {
        accessService.requireModeratorAccess(eventId, user);
        ForumPost post = requirePost(eventId, postId);
        post.setDeleted(true);
        post.setDeletedAt(Instant.now());
        post.setDeletedByUserId(user.getUserId());
        postRepo.save(post);
        eventPublisher.publishEvent(new ForumUpdatedEvent(eventId, "POST_DELETED", post.getPostId()));
    }

    @Transactional
    public void deleteComment(UUID eventId, UUID commentId, User user) {
        accessService.requireModeratorAccess(eventId, user);
        ForumComment comment = commentRepo.findById(commentId).orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        if (comment.isDeleted()) {
            throw new IllegalArgumentException("The comment: " + commentId + " could not be found");
        }

        requirePost(eventId, comment.getPostId());

        comment.setDeleted(true);
        comment.setDeletedAt(Instant.now());
        comment.setDeletedByUserId(user.getUserId());

        commentRepo.save(comment);

        eventPublisher.publishEvent(new ForumUpdatedEvent(eventId, "COMMENT_DELETED", comment.getCommentId()));
    }

    private ForumPost requirePost(UUID eventId, UUID postId) {
        return postRepo.findByPostIdAndEventIdAndIsDeletedFalse(postId, eventId).orElseThrow(() -> new IllegalArgumentException("Post: " + postId + " could not be found"));
    }

    private ForumPostSummaryResponse toPostSummary(ForumPost post) {
        long replyCount = commentRepo.countByPostIdAndIsDeletedFalse(post.getPostId());
        return new ForumPostSummaryResponse(post.getPostId(), post.getTitle(), getAuthor(post.getAuthorId()), post.getCreatedAt(), replyCount);
    }

    private ForumCommentResponse toCommentResponse(ForumComment comment) {
        return new ForumCommentResponse(comment.getCommentId(), comment.getPostId(), getAuthor(comment.getAuthorId()), comment.getBody(), comment.getCreatedAt());
    }

    private ForumAuthorResponse getAuthor(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("Forum author: " + userId + " could not be found"));
        String role = user.getRole() == null ? null : String.valueOf(user.getRole().getName());

        return new ForumAuthorResponse(user.getUserId(), user.getFirstName(), user.getLastName(), role);
    }
}