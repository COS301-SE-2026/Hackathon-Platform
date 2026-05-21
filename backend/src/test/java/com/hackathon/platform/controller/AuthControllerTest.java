package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.AuthResponse;
import com.hackathon.platform.dto.LoginRequest;
import com.hackathon.platform.dto.RegisterRequest;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objMapper;

  private RegisterRequest validRequest;

  @BeforeEach
  void setUp() {
    validRequest = new RegisterRequest("Jane", "Doe", "jane.doe@gmail.com", "TestPassword");
  }

  @Test
  void register_withValidPayload_returns201AndToken() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").exists())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    AuthResponse response = objMapper.readValue(content, AuthResponse.class);

    assertThat(response).isNotNull();
    assertThat(response.getToken()).isNotNull();
  }

  @Test
  void register_withMissingCredentials_returns400BadRequest() throws Exception {
    validRequest.setFirstName(null);
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(validRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_withDuplicateEmail_isRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(validRequest)))
        .andExpect(status().isConflict());
  }

  @Test
  void login_withCorrectCredentials_returns200CreatedAndToken() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated());

    LoginRequest loginReq = new LoginRequest("jane.doe@gmail.com", "TestPassword");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andReturn();

    AuthResponse response =
        objMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);

    assertThat(response).isNotNull();
    assertThat(response.getToken()).isNotBlank();
  }

  @Test
  void login_withWrongPassword_returns401Unauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated());
    LoginRequest loginReq = new LoginRequest("jane.doe@gmail.com", "TestPassworD");
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(loginReq)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void me_withAuthenticatedUser_returnsUserProfile() throws Exception {
    Role participantRole = Role.builder().roleId(2).name("PARTICIPANT").build();
    User user =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("Jane")
            .lastName("Doe")
            .email("jane@example.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(participantRole)
            .status("ACTIVE")
            .build();

    mockMvc
        .perform(
            get("/api/auth/me")
                .with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(
                            user, null, Collections.emptyList()))))
        .andExpect(status().isOk());
  }

  @Test
  void me_whenAnonymous_returnsAuthenticationFailureStatus() throws Exception {
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isForbidden());
  }
}
