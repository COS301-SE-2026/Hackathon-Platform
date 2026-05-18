package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.AuthResponse;
import com.hackathon.platform.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
    validRequest = new RegisterRequest("Jane", "Doe", "Jane.Doe@gmail.com", "TestPassword");
  }

  @Test
  void register_withValidPayload_returns201CreatedAndToken() throws Exception {
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
}
