package com.hackathon.platform.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.CreateAdminRequest;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
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
class SuperAdminControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objMapper;

  private CreateAdminRequest createAdminRequest;
  private UsernamePasswordAuthenticationToken superAdminAuth;
  private UsernamePasswordAuthenticationToken adminAuth;
  private UsernamePasswordAuthenticationToken participantAuth;

  @BeforeEach
  void setUp() {
    createAdminRequest = new CreateAdminRequest();
    createAdminRequest.setFirstName("Jane");
    createAdminRequest.setLastName("Doe");
    createAdminRequest.setEmail("jane.newadmin@example.com");
    createAdminRequest.setPassword("TestPassword1");

    Role superAdminRole = Role.builder().roleId(3).name("SUPERADMIN").build();
    User superAdminUser =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("Super")
            .lastName("Admin")
            .email("super@example.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(superAdminRole)
            .status("ACTIVE")
            .build();

    superAdminAuth =
        new UsernamePasswordAuthenticationToken(
            superAdminUser,
            null,
            List.of(
                new SimpleGrantedAuthority("ROLE_SUPERADMIN"),
                new SimpleGrantedAuthority("ROLE_ADMIN")));

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
            .firstName("John")
            .lastName("Smith")
            .email("john@example.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(participantRole)
            .status("ACTIVE")
            .build();
    participantAuth =
        new UsernamePasswordAuthenticationToken(
            participantUser, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
  }

  @Test
  void createAdmin_asSuperAdmin_returns201AndAuthResponse() throws Exception {
    mockMvc
        .perform(
            post("/api/superadmin/admin")
                .with(authentication(superAdminAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(createAdminRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("jane.newadmin@example.com"))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.userId").exists());
  }

  @Test
  void createAdmin_asAdmin_returns403Forbidden() throws Exception {

    mockMvc
        .perform(
            post("/api/superadmin/admin")
                .with(authentication(adminAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(createAdminRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  void createAdmin_asParticipant_returns403Forbidden() throws Exception {

    mockMvc
        .perform(
            post("/api/superadmin/admin")
                .with(authentication(participantAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(createAdminRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  void createAdmin_withMissingFields_returns400BadRequest() throws Exception {
    createAdminRequest.setEmail(null);

    mockMvc
        .perform(
            post("/api/superadmin/admin")
                .with(authentication(superAdminAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(createAdminRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createAdmin_withDuplicateEmail_returns409Conflict() throws Exception {

    mockMvc
        .perform(
            post("/api/superadmin/admin")
                .with(authentication(superAdminAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(createAdminRequest)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/superadmin/admin")
                .with(authentication(superAdminAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(createAdminRequest)))
        .andExpect(status().isConflict());
  }

  @Test
  void getAdmins_asSuperAdmin_returns200AndListContainingAdmin() throws Exception {

    mockMvc
        .perform(
            post("/api/superadmin/admin")
                .with(authentication(superAdminAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objMapper.writeValueAsString(createAdminRequest)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/superadmin/admins").with(authentication(superAdminAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.email == 'jane.newadmin@example.com')]").exists());
  }

  @Test
  void getAdmins_asAdmin_returns403Forbidden() throws Exception {
    mockMvc
        .perform(get("/api/superadmin/admins").with(authentication(adminAuth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAdmins_asParticipant_returns403Forbidden() throws Exception {

    mockMvc
        .perform(get("/api/superadmin/admins").with(authentication(participantAuth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAdmins_withoutAuthentication_returns403Forbidden() throws Exception {

    mockMvc.perform(get("/api/superadmin/admins")).andExpect(status().isForbidden());
  }
}
