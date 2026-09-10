package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.model.ForumPost;
import com.hackathon.platform.model.ForumComment;
import com.hackathon.platform.dto.CreateForumPostRequest;
import com.hackathon.platform.dto.CreateForumCommentRequest;
import com.hackathon.platform.dto.ForumCommentResponse;
import com.hackathon.platform.dto.ForumPostDetailResponse;
import com.hackathon.platform.dto.ForumPostSummaryResponse;
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.ForumPostRepository;
import com.hackathon.platform.repository.ForumCommentRepository;
import com.hackathon.platform.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ForumServiceTest {
    @Mock private ForumPostRepository postRepo;
    @Mock private ForumCommentRepository commentRepo;
    @Mock private UserRepository userRepo;
    @Mock private ForumAccessService forumAccSer;
    @Mock private ApplicationEventPublisher eventPublish;
    @InjectMocks private ForumService forumService;

    private UUID eventId;
    private UUID postId;
    private UUID commentId;
    private Event event;
    private User part;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        postId = UUID.randomUUID();
        commentId = UUID.randomUUID();

        Role partRole = Role.builder().roleId(1).name("PARTICIPANT").build();

        part = User.builder().userId(UUID.randomUUID()).firstName("Test").lastName("Part").role(partRole).build();

        event = new Event();
        event.setEventId(eventId);
    }

    @Test
    void createPost_validPost_returnsCreatedPost() {
        CreateForumPostRequest req = new CreateForumPostRequest();
        req.setTitle("test");
        req.setBody("body");

        when(userRepo.findById(part.getUserId())).thenReturn(Optional.of(part));
        when(postRepo.save(any(ForumPost.class))).thenAnswer(invocation -> {
            ForumPost post = invocation.getArgument(0);
            post.setPostId(postId);
            return post;
        });

        ForumPostDetailResponse res = forumService.createPost(eventId, part, req);

        assertThat(res.getPostId()).isEqualTo(postId);
        assertThat(res.getTitle()).isEqualTo("test");
        assertThat(res.getBody()).isEqualTo("body");
        assertThat(res.getComments()).isEmpty();

        verify(forumAccSer).requireForumAccess(eventId, part);
        verify(postRepo).save(any(ForumPost.class));
    }

    @Test
    void createComment_validComment_returnsCreatedComment() {
        CreateForumCommentRequest req = new CreateForumCommentRequest();
        req.setBody("body");
        ForumPost post = new ForumPost(eventId, part.getUserId(), "Test", "Post");
        post.setPostId(postId);
        when(postRepo.findByPostIdAndEventIdAndIsDeletedFalse(postId, eventId)).thenReturn(Optional.of(post));
        when(commentRepo.save(any(ForumComment.class))).thenAnswer(invocation -> {
            ForumComment comment = invocation.getArgument(0);
            comment.setCommentId(commentId);
            return comment;
        });

        when(userRepo.findById(part.getUserId())).thenReturn(Optional.of(part));
        ForumCommentResponse res = forumService.createComment(eventId, postId, part, req);
        assertThat(res.getCommentId()).isEqualTo(commentId);
        assertThat(res.getPostId()).isEqualTo(postId);
        assertThat(res.getBody()).isEqualTo("body");

        verify(forumAccSer).requireForumAccess(eventId, part);
        verify(commentRepo).save(any(ForumComment.class));
    }

    @Test
    void getPosts_success_returnsPosts() {
        ForumPost post = new ForumPost(eventId, part.getUserId(), "Test", "Post");
        post.setPostId(postId);

        when(postRepo.findByEventIdAndIsDeletedFalseOrderByCreatedAtDesc(eventId)).thenReturn(List.of(post));
        when(userRepo.findById(part.getUserId())).thenReturn(Optional.of(part));
        when(commentRepo.countByPostIdAndIsDeletedFalse(postId)).thenReturn(2L);

        List<ForumPostSummaryResponse> res = forumService.getPosts(eventId, part);
        
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getPostId()).isEqualTo(postId);
        assertThat(res.get(0).getTitle()).isEqualTo("Test");
        assertThat(res.get(0).getReplyCount()).isEqualTo(2);
        assertThat(res.get(0).getAuthor().getUserId()).isEqualTo(part.getUserId());
        verify(forumAccSer).requireForumAccess(eventId, part);
        verify(postRepo).findByEventIdAndIsDeletedFalseOrderByCreatedAtDesc(eventId);
    }

    @Test
    void getPost_success_returnsPostsWithComments() {
        ForumPost post = new ForumPost(eventId, part.getUserId(), "Test", "Post");
        post.setPostId(postId);

        ForumComment comment = new ForumComment(postId, part.getUserId(), "COMMENT");
        comment.setCommentId(commentId);

        when(postRepo.findByPostIdAndEventIdAndIsDeletedFalse(postId, eventId)).thenReturn(Optional.of(post));
        when(commentRepo.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId)).thenReturn(List.of(comment));
        when(userRepo.findById(part.getUserId())).thenReturn(Optional.of(part));

        ForumPostDetailResponse res = forumService.getPost(eventId, postId, part);
        
        assertThat(res.getPostId()).isEqualTo(postId);
        assertThat(res.getTitle()).isEqualTo("Test");
        assertThat(res.getBody()).isEqualTo("Post");
        assertThat(res.getComments()).hasSize(1);
        assertThat(res.getComments().get(0).getCommentId()).isEqualTo(commentId);
        assertThat(res.getComments().get(0).getBody()).isEqualTo("COMMENT");

        verify(forumAccSer).requireForumAccess(eventId, part);
        verify(postRepo).findByPostIdAndEventIdAndIsDeletedFalse(postId, eventId);
        verify(commentRepo).findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId);
    }

    @Test
    void getPost_postNotFound_throwsException() {
        when(postRepo.findByPostIdAndEventIdAndIsDeletedFalse(postId, eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> forumService.getPost(eventId, postId, part)).isInstanceOf(IllegalArgumentException.class);

        verify(forumAccSer).requireForumAccess(eventId, part);
        verify(postRepo).findByPostIdAndEventIdAndIsDeletedFalse(postId, eventId);
        verify(commentRepo, never()).findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId);
    }
}