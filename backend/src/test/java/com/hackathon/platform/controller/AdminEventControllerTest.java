package com.hackathon.platform.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.EventRequest;
import com.hackathon.platform.dto.AuthResponse;
import com.hackathon.platform.dto.RegisterRequest;
import com.hackathon.platform.dto.LoginRequest;
import com.hackathon.platform.model.User;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AdminEventControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objMapper;

    private EventRequest eventRequest;
    private UsernamePasswordAuthenticationToken adminAuth;
    private UsernamePasswordAuthenticationToken participantAuth;
    private UUID eventId;
    private UUID creatorUserId;
    private Event event;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        creatorUserId = UUID.randomUUID();
        event = new Event();
        event.setEventId(eventId);
        event.setCreatedByUserId(creatorUserId);
        event.setName("Test Hackathon");
        event.setVisibility("PUBLIC");
        event.setStatus("INACTIVE");
        event.setRegistrationKey(null);
        event.setDuration(400);
        event.setStartDateTime(OffsetDateTime.now().plusDays(7));
        event.setTeamSizeLimit((short) 5);

        Role adminRole = Role.builder().roleId(1).name("ADMIN").build();
        User adminUser = User.builder().userId(UUID.randomUUID()).firstName("Jane").lastName("Doe").email("janeAdmin@example.com").passwordHash("$2a$12$hashedpassword").role(adminRole).status("ACTIVE").build();
        adminAuth = new UsernamePasswordAuthenticationToken(adminUser, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        Role participantRole = Role.builder().roleId(2).name("PARTICIPANT").build();
        User participantUser = User.builder().userId(UUID.randomUUID()).firstName("Jane").lastName("Doe").email("jane@example.com").passwordHash("$2a$12$hashedpassword").role(participantRole).status("ACTIVE").build();
        participantAuth = new UsernamePasswordAuthenticationToken(participantUser, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
    }

    @Test
    void createEvent_asAdmin_returns200Ok() throws Exception {
        mockMvc.perform(post("/api/admin/events").with(authentication(adminAuth)).contentType(MediaType.APPLICATION_JSON).content(objMapper.writeValueAsString(event))).andExpect(status().isOk());
    }

    @Test
    void createEvent_asParticipant_returns403Forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/events").with(authentication(participantAuth)).contentType(MediaType.APPLICATION_JSON).content(objMapper.writeValueAsString(event))).andExpect(status().isForbidden());
    }
}