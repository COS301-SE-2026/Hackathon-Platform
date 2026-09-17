package com.hackathon.platform.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.hackathon.platform.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objMapper;
  @MockBean
  private AuthService authService;
  private RegisterRequest validRequest;
  private AuthResponse regResp;
  private AuthResponse loginResp;
  private AuthResponse verificationResp;

  @BeforeEach
  void setUp() {
    validRequest = new RegisterRequest("Donald", "Trump", "donald@gmail.com", "TestPassword");
    regResp = AuthResponse.builder().token("mock.jwt.token").userId(UUID.randomUUID()).firstName("Donald").lastName("Trump").email("donald@gmail.com").role("PARTICIPANT").emailVerified(true).build();
    verificationResp = AuthResponse.builder().token("verified.jwt.token").userId(UUID.randomUUID()).firstName("Donald").lastName("Trump").email("donald@gmail.com").role("PARTICIPANT").emailVerified(true).msg("Email verification complete").build();
    loginResp = AuthResponse.builder().token("mock.jwt.token").userId(UUID.randomUUID()).firstName("Donald").lastName("Trump").email("donald@gmail.com").role("PARTICIPANT").emailVerified(true).build();
  }

  @Test
  void register_withValidPayload_returns201() throws Exception {
    when(authService.register(any(RegisterRequest.class))).thenReturn(regResp);

        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.email").value("donald@gamil.com"))
                .andExpect(jsonPath("$.role").value("PARTICIPANT"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.msg").exists());

    verify(authService).register(any(RegisterRequest.class));
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
  void loginWithCreds_returns200() throws Exception{
    LoginRequest loginReq = new LoginRequest("donald@gmail.com", "TestPassword");
    when(authService.login(any(LoginRequest.class))).thenReturn(loginResp);
    mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objMapper.writeValueAsString(loginReq))).andExpect(status().isOk()).andExpect(jsonPath("$.token").value("mock.jwt.token")).andExpect(jsonPath("$.emailVerified").value(true));
    verify(authService).login(any(LoginRequest.class));
  }

  @Test
  void verifyEmailWithToken_return200() throws Exception{
    when(authService.verifyEmail("valid-token")).thenReturn(verificationResp);

    mockMvc.perform(get("/api/auth/verify-email").param("token", "valid-token")).andExpect(status().isOk()).andExpect(jsonPath("$.token").value("verified.jwt.token")).andExpect(jsonPath("$.emailVerification").value(true));
    verify(authService).verifyEmail("valid-token");
  }

  @Test
  void resendVerificationWithEmail_returns204() throws Exception{
    mockMvc.perform(post("/api/auth/resend-verification").param("email", "donald@gmail.com")).andExpect(status().isNoContent());
    verify(authService).resendVerificationEmail("donald@gmail.com");
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
    AuthResponse meResp = AuthResponse.builder().token("mock.jwt.token").userId(user.getUserId()).firstName(user.getFirstName()).lastName(user.getLastName()).email(user.getEmail()).role("PARTICIPANT").emailVerified(true).build();
    when(authService.getMe(any(User.class))).thenReturn(meResp);

    mockMvc
        .perform(
            get("/api/auth/me")
                .with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(
                            user, null, Collections.emptyList()))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("donald@gmail.com")).andExpect(jsonPath("$.emailVerified").value(true));
  }

  @Test
  void me_whenAnonymous_returnsAuthenticationFailureStatus() throws Exception {
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isForbidden());
  }
}
