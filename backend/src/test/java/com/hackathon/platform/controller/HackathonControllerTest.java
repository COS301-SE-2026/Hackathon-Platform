package com.hackathon.platform.controller;

import static
org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.HackathonRequest;
import com.hackathon.platform.model.Hackathon;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.HackathonRepository;
import com.hackathon.platform.repository.RoleRepository;
import com.hackathon.platform.repository.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class HackathonControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private HackathonRepository hackathonRepository;
  private HackathonRequest hackathonRequest;
  private UsernamePasswordAuthenticationToken admin;
  private UsernamePasswordAuthenticationToken partic;
  private UUID seededHackathonId;

  @BeforeEach
  void setUp() {
    Role adminRole = Role.builder().roleId(1).name("ADMIN").build();
    roleRepository.saveAndFlush(adminRole);
    Role partiRole = Role.builder().roleId(2).name("PARTICIPANT").build();
    roleRepository.saveAndFlush(partiRole);

    User ad =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("Abhishek")
            .lastName("Bachan")
            .email("abachan@gmail.com")
            .passwordHash("($2a$12$hashedpassword")
            .role(adminRole)
            .status("ACTIVE")
            .build();
    User savedAdmin = userRepository.saveAndFlush(ad);

    User parti =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("Janhvi")
            .lastName("Kapoor")
            .email("jk@gmail.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(partiRole)
            .status("ACTIVE")
            .build();
    User savedParti = userRepository.saveAndFlush(parti);
    admin =
        new UsernamePasswordAuthenticationToken(
            savedAdmin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    partic =
        new UsernamePasswordAuthenticationToken(
            savedParti, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));

    Hackathon seededHack = new Hackathon();
    seededHack.setName("Seeded hackathon");
    seededHack.setDescription("Seeeded for test for controler");
    Hackathon savedHack = hackathonRepository.saveAndFlush(seededHack);
    seededHackathonId = savedHack.getHackathonId();
    hackathonRequest = new HackathonRequest();
    hackathonRequest.setName("New");
    hackathonRequest.setDescription("New Description");
  }

  @Test
  void createHackathon_asAdmin_return200() throws Exception {
    mockMvc
        .perform(
            post("/api/hackathon")
                .with(authentication(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hackathonRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hackathonId").exists())
        .andExpect(jsonPath("$.name").value("New"));
  }

  @Test
  void createHackathon_asAdmin_return403() throws Exception {
    mockMvc
        .perform(
            post("/api/hackathon")
                .with(authentication(partic))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hackathonRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAllHackathons_returnSeededHackathon() throws Exception {
    mockMvc
        .perform(get("/api/hackathon").with(authentication(partic)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.hackathonId == '" + seededHackathonId + "')]").exists());
  }

  @Test
  void getHackathonById_returnSeededHackathon() throws Exception {
    mockMvc
        .perform(
            get("/api/hackathon/{hackathonId}", seededHackathonId).with(authentication(partic)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Seeded hackathon"));
  }

  @Test
  void updateHackathon_asAdmin_return200() throws Exception {
    HackathonRequest updateReq = new HackathonRequest();
    updateReq.setName("updated name");
    updateReq.setDescription("updated description");

    mockMvc
        .perform(
            put("/api/hackathon/{hackathonId}", seededHackathonId)
                .with(authentication(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("updated name"));
  }

  @Test
  void updateHackathon_asParticipant_return403() throws Exception {
    mockMvc
        .perform(
            put("/api/hackathon/{hackathonId}", seededHackathonId)
                .with(authentication(partic))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hackathonRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteHackathon_asAdmin_return204() throws Exception {
    mockMvc
        .perform(
            delete("/api/hackathon/{hackathonId}",
seededHackathonId).with(authentication(admin)))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteHackathon_asParticipant_return403() throws Exception {
    mockMvc
        .perform(
            delete("/api/hackathon/{hackathonId}",
seededHackathonId).with(authentication(partic)))
        .andExpect(status().isForbidden());
  }
}
