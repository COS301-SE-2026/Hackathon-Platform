package com.hackathon.platform.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.EventRequest;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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
    eventRequest = new EventRequest();
    eventRequest.setName("Test Hackathon");
    eventRequest.setVisibility("PUBLIC");
    eventRequest.setStatus("INACTIVE");
    eventRequest.setRegistrationKey(null);
    eventRequest.setDuration(400);
    eventRequest.setStartDateTime(OffsetDateTime.now().plusDays(7));
    eventRequest.setTeamSizeLimit((short) 5);

    Role adminRole = Role.builder().roleId(1).name("ADMIN").build();
    User adminUser =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("Jane")
            .lastName("Doe")
            .email("janeAdmin@example.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(adminRole)
            .status("ACTIVE")
            .build();
    adminAuth =
        new UsernamePasswordAuthenticationToken(
            adminUser, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    Role participantRole = Role.builder().roleId(2).name("PARTICIPANT").build();
    User participantUser =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("Jane")
            .lastName("Doe")
            .email("jane@example.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(participantRole)
            .status("ACTIVE")
            .build();
    participantAuth =
        new UsernamePasswordAuthenticationToken(
            participantUser, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
  }

  @Test
  void createEvent_asAdmin_returns200Ok() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/events")
                .with(authentication(adminAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(eventRequest)))
        .andExpect(status().isOk());
  }

  @Test
  void createEvent_asParticipant_returns403Forbidden() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/events")
                .with(authentication(participantAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(eventRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getEventByCreator_asAdmin_return200Ok() throws Exception {
    mockMvc
        .perform(get("/api/admin/events/{id}", creatorUserId).with(authentication(adminAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void getEventByCreator_asParticipant_return403Forbidden() throws Exception {
    mockMvc
        .perform(get("/api/admin/events/{id}", creatorUserId).with(authentication(participantAuth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void putUpdateEvent_asAdmin_returns200Ok() throws Exception {
    MvcResult newEvent =
        mockMvc
            .perform(
                post("/api/admin/events")
                    .with(authentication(adminAuth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objMapper.writeValueAsString(eventRequest)))
            .andExpect(status().isOk())
            .andReturn();

    String content = newEvent.getResponse().getContentAsString();
    Event exisitngEvent = objMapper.readValue(content, Event.class);
    UUID exisitngEventId = exisitngEvent.getEventId();

    eventRequest.setName("New name");
    mockMvc
        .perform(
            put("/api/admin/events/{id}", exisitngEventId)
                .with(authentication(adminAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(eventRequest)))
        .andExpect(status().isOk());
  }

  @Test
  void putUpdateEvent_asParticipant_returns403Forbidden() throws Exception {
    mockMvc
        .perform(
            put("/api/admin/events/{id}", eventId)
                .with(authentication(participantAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(eventRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  void patchEventStatus_asAdmin_returns200Ok() throws Exception {
    MvcResult newEvent =
        mockMvc
            .perform(
                post("/api/admin/events")
                    .with(authentication(adminAuth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objMapper.writeValueAsString(eventRequest)))
            .andExpect(status().isOk())
            .andReturn();

    String content = newEvent.getResponse().getContentAsString();
    Event exisitngEvent = objMapper.readValue(content, Event.class);
    UUID exisitngEventId = exisitngEvent.getEventId();

    eventRequest.setVisibility("PRIVATE");
    eventRequest.setStatus("ACTIVE");
    eventRequest.setRegistrationKey("TESTPASSWORD");

    mockMvc
        .perform(
            patch("/api/admin/events/{id}/status", exisitngEventId)
                .with(authentication(adminAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(eventRequest)))
        .andExpect(status().isOk());
  }

  @Test
  void patchEventStatus_asParticipant_returns403Forbidden() throws Exception {
    mockMvc
        .perform(
            patch("/api/admin/events/{id}/status", eventId)
                .with(authentication(participantAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(eventRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getEventStatus_asAdmin_returns200Ok() throws Exception {
    MvcResult newEvent =
        mockMvc
            .perform(
                post("/api/admin/events")
                    .with(authentication(adminAuth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objMapper.writeValueAsString(eventRequest)))
            .andExpect(status().isOk())
            .andReturn();

    String content = newEvent.getResponse().getContentAsString();
    Event exisitngEvent = objMapper.readValue(content, Event.class);
    UUID exisitngEventId = exisitngEvent.getEventId();

    mockMvc
        .perform(
            get("/api/admin/events/{id}/status", exisitngEventId).with(authentication(adminAuth)))
        .andExpect(status().isOk());
  }

  @Test
  void getEventStatus_asParticipant_returns403isForbidden() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/events/{id}/status", eventId).with(authentication(participantAuth)))
        .andExpect(status().isForbidden());
  }
}
