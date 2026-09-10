package com.hackathon.platform.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.dto.ForumAuthorResponse;
import com.hackathon.platform.dto.ForumPostSummaryResponse;
import com.hackathon.platform.dto.ForumPostDetailResponse;
import com.hackathon.platform.dto.CreateForumPostRequest;
import com.hackathon.platform.dto.CreateForumCommentRequest;
import com.hackathon.platform.dto.ForumPermissionResponse;
import com.hackathon.platform.repository.UserRepository;
import com.hackathon.platform.service.ForumService;
import com.hackathon.platform.service.ForumAccessService;
import com.hackathon.platform.service.ForumUpdateService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ForumControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objMapper;
    
    @MockBean private ForumService forumService;
    @MockBean private ForumAccessService forumAccSer;
    @MockBean private ForumUpdateService forumUpSer;
    @MockBean private UserRepository userRepo;

    private UUID eventId;
    private UUID postId;
    private UUID commentId;
    private User part;
    private User admin;
    private UsernamePasswordAuthenticationToken partAuth;
    private UsernamePasswordAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        postId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        Role partRole = Role.builder().roleId(2).name("PARTICIPANT").build();
        Role adminRole = Role.builder().roleId(11).name("ADMIN").build();
        part = User.builder().userId(UUID.randomUUID()).firstName("Test").lastName("Part").email("test@test.com").passwordHash("hash").status("ACTIVE").role(partRole).build();
        admin = User.builder().userId(UUID.randomUUID()).firstName("Test").lastName("Admin").email("testA@test.com").passwordHash("hash").status("ACTIVE").role(adminRole).build();
        partAuth = new UsernamePasswordAuthenticationToken(part, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
        adminAuth = new UsernamePasswordAuthenticationToken(admin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void getPosts_returns200() throws Exception {
        ForumAuthorResponse author = new ForumAuthorResponse(part.getUserId(), "Test", "Part", "PARTICIPANT");
        ForumPostSummaryResponse post = new ForumPostSummaryResponse(postId, "TEST", author, null, 2L);

        when(forumService.getPosts(eventId, part)).thenReturn(List.of(post));

        mockMvc.perform(get("/api/events/{id}/forum/posts", eventId).with(authentication(partAuth))).andExpect(status().isOk());
        verify(forumService).getPosts(eventId, part);
    }

    @Test
    void getPost_returns200() throws Exception {
        ForumAuthorResponse author = new ForumAuthorResponse(part.getUserId(), "Test", "Part", "PARTICIPANT");
        ForumPostDetailResponse post = new ForumPostDetailResponse(postId, "TEST", "BODY", author, null, List.of());

        when(forumService.getPost(eventId, postId, part)).thenReturn(post);

        mockMvc.perform(get("/api/events/{eId}/forum/posts/{pId}", eventId, postId).with(authentication(partAuth))).andExpect(status().isOk());

        verify(forumService).getPost(eventId, postId, part);
    }

    @Test
    void createPost_Returns201() throws Exception {
        String body = """
            {
                "title": "TEST",
                "body": "body"    
            }
                    """;
        mockMvc.perform(post("/api/events/{id}/forum/posts", eventId).with(authentication(partAuth)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());

        verify(forumService).createPost(eq(eventId), eq(part), any(CreateForumPostRequest.class));
    }

    @Test
    void createInvalidPost_returns400() throws Exception {
        String body = """
            {
                "title": "",
                "body": ""    
            }
                    """;
        mockMvc.perform(post("/api/events/{id}/forum/posts", eventId).with(authentication(partAuth)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());

        verify(forumService, never()).createPost(eq(eventId), eq(part), any(CreateForumPostRequest.class));
    }

    @Test
    void createComment_returns201() throws Exception {
        String body = """
            {
                "body": "body"    
            }
                    """;
        mockMvc.perform(post("/api/events/{id}/forum/posts/{postId}/comments", eventId, postId).with(authentication(partAuth)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());

        verify(forumService).createComment(eq(eventId), eq(postId), eq(part), any(CreateForumCommentRequest.class));
    }

    @Test
    void createInvalidComment_returns400() throws Exception {
        String body = """
            {
                "body": ""    
            }
                    """;
        mockMvc.perform(post("/api/events/{id}/forum/posts/{postId}/comments", eventId, postId).with(authentication(partAuth)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());

        verify(forumService, never()).createComment(eq(eventId), eq(postId), eq(part), any(CreateForumCommentRequest.class));
    }

    @Test
    void getPermissions_returns200() throws Exception {
        ForumPermissionResponse perms = new ForumPermissionResponse(true, true, false);

        when(forumAccSer.getPermissions(eventId, part)).thenReturn(perms);

        mockMvc.perform(get("/api/events/{id}/forum/permissions", eventId).with(authentication(partAuth))).andExpect(status().isOk());
        verify(forumAccSer).getPermissions(eventId, part);
    }

    @Test
    void deletePost_returns204() throws Exception {
        mockMvc.perform(delete("/api/events/{id}/forum/posts/{postId}", eventId, postId).with(authentication(partAuth))).andExpect(status().isNoContent());
        verify(forumService).deletePost(eventId, postId, part);
    }

    @Test
    void deleteComment_returns204() throws Exception {
        mockMvc.perform(delete("/api/events/{id}/forum/comments/{commentId}", eventId, commentId).with(authentication(partAuth))).andExpect(status().isNoContent());
        verify(forumService).deleteComment(eventId, commentId, part);
    }

    @Test
    void deletePost_admin_return204() throws Exception {
        mockMvc.perform(delete("/api/admin/events/{eid}/forum/posts/{pid}",eventId, postId).with(authentication(adminAuth))).andExpect(status().isNoContent());
        verify(forumService).deletePost(eventId, postId, admin);
    }

    @Test
    void deleteComment_admin_returns204() throws Exception {
        mockMvc.perform(delete("/api/admin/events/{eventId}/forum/comments/{comId}", eventId, commentId).with(authentication(adminAuth))).andExpect(status().isNoContent());
        verify(forumService).deleteComment(eventId, commentId, admin);
    }

    @Test
    void deletePost_participant_returns403() throws Exception {
        mockMvc.perform(delete("/api/admin/events/{eid}/forum/posts/{pid}",eventId, postId).with(authentication(partAuth))).andExpect(status().isForbidden());
        verify(forumService, never()).deleteComment(eventId, commentId, admin);
    }
}