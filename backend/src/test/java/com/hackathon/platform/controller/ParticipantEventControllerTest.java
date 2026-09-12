package com.hackathon.platform.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.EventRegistrationRequest;
import com.hackathon.platform.dto.EventRegistrationResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CertificateService;
import com.hackathon.platform.service.EventRegistrationService;
import com.hackathon.platform.service.EventService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ParticipantEventControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objMapper;
    @MockBean private EventService eventService;
    @MockBean private EventRegistrationService eventRegService;
    @MockBean private CertificateService certServ;
    private UUID eventId;
    private UUID userId;
    private User user;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp(){
        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();
        user = User.builder().userId(userId).firstName("Varun").lastName("Dhawan").email("varunDhawan@gmail.com").passwordHash("helloIfYoureReadingThis").status("ACTIVE").role(Role.builder().roleId(2).name("PARTICIPANT").build()).build();
        auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
    }
}