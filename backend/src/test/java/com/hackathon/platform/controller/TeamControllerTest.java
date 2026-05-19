package com.hackathon.platform.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.AuthResponse;
import com.hackathon.platform.dto.ApproveRequest;
import com.hackathon.platform.dto.CreateTeamRequest;
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
class TeamControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objMapper;

    private CreateTeamRequest createTeamRequest;
    private ApproveRequest approveRequest;

    private UUID teamId;
    private UUID userId;
    private UUID eventId;

    private UsernamePasswordAuthenticationToken userAuth;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        createTeamRequest = new CreateTeamRequest();
        createTeamRequest.setTeamName("Test Team");
        createTeamRequest.setEventId(eventId);

        approveRequest = new ApproveRequest();
        approveRequest.setApprove(true);

        Role participantRole = Role.builder().roleId(2).name("PARTICIPANT").build();
        userAuth = new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
    }

    @Test 
    void createTeam_returns201Created() throws Exception {
        mockMvc.perform(post("/api/teams").with(authentication(userAuth)).contentType(MediaType.APPLICATION_JSON).content(objMapper.writeValueAsString(createTeamRequest))).andExpect(status().isCreated()).andExpect(jsonPath("$.teamId").exists());
    }
}