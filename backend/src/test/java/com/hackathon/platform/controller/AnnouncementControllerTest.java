package com.hackathon.platform.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.dto.CreateAnnouncementRequest;
import com.hackathon.platform.repository.UserRepository;
import com.hackathon.platform.service.AnnouncementService;
import com.hackathon.platform.service.AnnouncementUpdateService;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AnnouncementControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AnnouncementService annSer;
    @MockBean private AnnouncementUpdateService annUpSer;
    @MockBean private UserRepository userRepo;

    private UUID eventId;
    private User part;
    private User admin;

    private UsernamePasswordAuthenticationToken partAuth;
    private UsernamePasswordAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        Role partRole = Role.builder().roleId(2).name("PARTICIPANT").build();
        Role adminRole = Role.builder().roleId(11).name("ADMIN").build();
        part = User.builder().userId(UUID.randomUUID()).firstName("Test").lastName("Part").email("test@test.com").passwordHash("hash").status("ACTIVE").role(partRole).build();
        admin = User.builder().userId(UUID.randomUUID()).firstName("Test").lastName("Admin").email("testA@test.com").passwordHash("hash").status("ACTIVE").role(adminRole).build();
        partAuth = new UsernamePasswordAuthenticationToken(part, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
        adminAuth = new UsernamePasswordAuthenticationToken(admin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void getAnnouncements_participants_returns200() throws Exception {
        mockMvc.perform(get("/api/events/{id}/announcements", eventId).with(authentication(partAuth))).andExpect(status().isOk());
        verify(annSer).getAnnouncements(eventId, part.getUserId());
    }

    @Test
     void getAnnouncements_admins_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/events/{id}/announcements", eventId).with(authentication(adminAuth))).andExpect(status().isOk());
        verify(annSer).getAnnouncementsForAdmin(eventId, admin.getUserId());
    }

    @Test
    void createAnnouncement_admin_returns201() throws Exception {
        String body =   """
                        {
                            "title": "Important",
                            "body": "body",
                            "severity": "INFO"
                        }
                        """;

        mockMvc.perform(post("/api/admin/events/{id}/announcements", eventId).with(authentication(adminAuth)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        verify(annSer).createAnnouncement(eq(eventId),eq(admin.getUserId()), any(CreateAnnouncementRequest.class));
    }

    @Test
    void createInvalidAnnouncement_admin_returns400() throws Exception {
        String body =   """
                        {
                            "title": "",
                            "body": ""
                        }
                        """;

        mockMvc.perform(post("/api/admin/events/{id}/announcements", eventId).with(authentication(adminAuth)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verify(annSer, never()).createAnnouncement(eq(eventId),eq(admin.getUserId()), any(CreateAnnouncementRequest.class));
    }

    @Test
    void createAnnouncement_participant_returns403() throws Exception {
        String body =   """
                        {
                            "title": "Important",
                            "body": "body",
                            "severity": "INFO"
                        }
                        """;

        mockMvc.perform(post("/api/admin/events/{id}/announcements", eventId).with(authentication(partAuth)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        verify(annSer, never()).createAnnouncement(eq(eventId),eq(part.getUserId()), any(CreateAnnouncementRequest.class));
    }
}