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
                new SimpleGrantedAuthority("ROLE_ADMIN")
            )
        );
    
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
            adminUser, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    
    Role participantRole = Role.builder().roleId.name("PARTICIPANT").build();
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
            participantUser, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT"))
        );
  }

  


}