package com.hackathon.platform.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.ApproveRequest;
import com.hackathon.platform.dto.CreateTeamRequest;
import com.hackathon.platform.dto.TeamResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.RoleRepository;
import com.hackathon.platform.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class TeamControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private EventRepository eventRepository;

  private CreateTeamRequest createTeamRequest;
  private ApproveRequest approveRequest;

  private UUID userId;
  private UUID eventId;

  private UsernamePasswordAuthenticationToken userAuth;

  @BeforeEach
  void setUp() {
    Role participantRole = Role.builder().roleId(2).name("PARTICIPANT").build();
    Role savedParticipantRole = roleRepository.saveAndFlush(participantRole);

    User user =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("Jane")
            .lastName("Doe")
            .email("jane@example.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(savedParticipantRole)
            .status("ACTIVE")
            .build();

    User savedUser = userRepository.saveAndFlush(user);
    userId = savedUser.getUserId();

    Event event = new Event();
    event.setCreatedByUserId(userId);
    event.setName("Test event");
    event.setVisibility("PUBLIC");
    event.setStatus("ACTIVE");
    event.setDuration(4000);
    event.setStartDateTime(OffsetDateTime.now().plusDays(7));
    event.setTeamSizeLimit((short) 3);

    Event savedEvent = eventRepository.saveAndFlush(event);
    eventId = savedEvent.getEventId();

    createTeamRequest = new CreateTeamRequest();
    createTeamRequest.setTeamName("Test Team");
    createTeamRequest.setEventId(eventId);

    approveRequest = new ApproveRequest();
    approveRequest.setApprove(true);

    userAuth =
        new UsernamePasswordAuthenticationToken(
            userId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
  }

  @Test
  void createTeam_returns201Created() throws Exception {
    mockMvc
        .perform(
            post("/api/teams")
                .with(authentication(userAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(createTeamRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.teamId").exists());
  }

  @Test
  void requestToJoin_returns201Created() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/teams")
                    .with(authentication(userAuth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objMapper.writeValueAsString(createTeamRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    TeamResponse response = objMapper.readValue(responseBody, TeamResponse.class);
    UUID createdTeamId = response.getTeamId();

    User member =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("John")
            .lastName("Smith")
            .email("john@gmail.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(roleRepository.findByName("PARTICIPANT").orElse(null))
            .status("ACTIVE")
            .build();
    User savedMember = userRepository.saveAndFlush(member);
    UUID memberId = savedMember.getUserId();

    UsernamePasswordAuthenticationToken memberAuth =
        new UsernamePasswordAuthenticationToken(
            memberId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));

    mockMvc
        .perform(
            post("/api/teams/{id}/join-requests", createdTeamId).with(authentication(memberAuth)))
        .andDo(print())
        .andExpect(status().isCreated());
  }

  @Test
  void viewTeam_returns200Ok() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/teams")
                    .with(authentication(userAuth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objMapper.writeValueAsString(createTeamRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    TeamResponse response = objMapper.readValue(responseBody, TeamResponse.class);
    UUID createdTeamId = response.getTeamId();

    mockMvc
        .perform(get("/api/teams/{id}/members", createdTeamId).with(authentication(userAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void approveOrRejectJoinRequest_returns200Ok() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/teams")
                    .with(authentication(userAuth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objMapper.writeValueAsString(createTeamRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    TeamResponse response = objMapper.readValue(responseBody, TeamResponse.class);
    UUID createdTeamId = response.getTeamId();

    User member =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("John")
            .lastName("Smith")
            .email("john@gmail.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(roleRepository.findByName("PARTICIPANT").orElse(null))
            .status("ACTIVE")
            .build();
    User savedMember = userRepository.saveAndFlush(member);
    UUID memberId = savedMember.getUserId();

    UsernamePasswordAuthenticationToken memberAuth =
        new UsernamePasswordAuthenticationToken(
            memberId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));

    mockMvc
        .perform(
            post("/api/teams/{id}/join-requests", createdTeamId).with(authentication(memberAuth)))
        .andDo(print())
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            put("/api/teams/{teamId}/join-requests/{userId}", createdTeamId, memberId)
                .with(authentication(userAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(approveRequest)))
        .andExpect(status().isOk());
  }

  @Test
  void leaveTeam_returns204NoContent() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/teams")
                    .with(authentication(userAuth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objMapper.writeValueAsString(createTeamRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    TeamResponse response = objMapper.readValue(responseBody, TeamResponse.class);
    UUID createdTeamId = response.getTeamId();

    User member =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("John")
            .lastName("Smith")
            .email("john@gmail.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(roleRepository.findByName("PARTICIPANT").orElse(null))
            .status("ACTIVE")
            .build();
    User savedMember = userRepository.saveAndFlush(member);
    UUID memberId = savedMember.getUserId();

    UsernamePasswordAuthenticationToken memberAuth =
        new UsernamePasswordAuthenticationToken(
            memberId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));

    mockMvc
        .perform(
            post("/api/teams/{id}/join-requests", createdTeamId).with(authentication(memberAuth)))
        .andDo(print())
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            put("/api/teams/{teamId}/join-requests/{userId}", createdTeamId, memberId)
                .with(authentication(userAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(approveRequest)))
        .andExpect(status().isOk());

    mockMvc
        .perform(delete("/api/teams/{id}/members", createdTeamId).with(authentication(memberAuth)))
        .andDo(print())
        .andExpect(status().isNoContent());
  }
}
