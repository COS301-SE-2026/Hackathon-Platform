package com.hackathon.platform.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.PlagiarismDiffResponse;
import com.hackathon.platform.dto.PlagiarismRunRequest;
import com.hackathon.platform.dto.SubmissionSimilarityResponse;
import com.hackathon.platform.model.PlagiarismRun;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.plagiarism.PlagiarismCheckService;
import com.hackathon.platform.plagiarism.queue.PlagiarismJobProducer;
import com.hackathon.platform.repository.PlagiarismRunRepository;
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
class PlagiarismControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private PlagiarismJobProducer producer;
  @MockBean private PlagiarismRunRepository runRepo;
  @MockBean private PlagiarismCheckService checkService;

  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  private UsernamePasswordAuthenticationToken adminAuth;
  private UsernamePasswordAuthenticationToken participantAuth;
  private User adminUser;

  @BeforeEach
  void setUp() {
    adminUser =
        User.builder()
            .userId(USER_ID)
            .firstName("Admin")
            .lastName("User")
            .email("admin@test.com")
            .passwordHash("hash")
            .status("ACTIVE")
            .role(Role.builder().roleId(1).name("ADMIN").build())
            .build();

    User participantUser =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("Participant")
            .lastName("User")
            .email("participant@test.com")
            .passwordHash("hash")
            .status("ACTIVE")
            .role(Role.builder().roleId(2).name("PARTICIPANT").build())
            .build();

    adminAuth =
        new UsernamePasswordAuthenticationToken(
            adminUser, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    participantAuth =
        new UsernamePasswordAuthenticationToken(
            participantUser, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
  }

  @Test
  void triggerRun_asAdmin_returns202WithRunId() throws Exception {
    
    PlagiarismRunRequest request = new PlagiarismRunRequest((short) 3, 15);
    when(producer.enqueue(eq(EVENT_ID), eq(Short.valueOf((short) 3)), eq(15), eq(USER_ID)))
        .thenReturn(42L);

    mockMvc
        .perform(
            post("/api/admin/events/{eventId}/plagiarism/runs", EVENT_ID)
                .with(authentication(adminAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.runId").value(42));

  }

  @Test
  void triggerRun_withNoBody_usesDefaultTopN() throws Exception {

    when(producer.enqueue(eq(EVENT_ID), isNull(), anyInt(), eq(USER_ID))).thenReturn(7L);

    mockMvc
        .perform(
            post("/api/admin/events/{eventId}/plagiarism/runs", EVENT_ID)
                .with(authentication(adminAuth))
        )
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.runId").value(7));

  }

  @Test
  void triggerRun_asParticipant_returns403() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/events/{eventId}/plagiarism/runs", EVENT_ID)
                .with(authentication(participantAuth))
        )
        .andExpect(status().isForbidden());

  }

  @Test
  void listRuns_asAdmin_returnsRunsOrderedByRequestedAt() throws Exception {
    PlagiarismRun run = new PlagiarismRun(EVENT_ID, (short) 1, 20, USER_ID);
    when(runRepo.findByEventIdOrderByRequestedAtDesc(EVENT_ID)).thenReturn(List.of(run));

    mockMvc
        .perform(
            get("/api/admin/events/{eventId}/plagiarism/runs", EVENT_ID)
                .with(authentication(adminAuth))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].status").value("QUEUED"))
        .andExpect(jsonPath("$[0].topN").value(20));

  }

  @Test
  void listRuns_asParticipant_returns403() throws Exception {

    mockMvc
        .perform(
            get("/api/admin/events/{eventId}/plagiarism/runs", EVENT_ID)
                .with(authentication(participantAuth))
        )
        .andExpect(status().isForbidden());

  }

  @Test
  void getPairs_asAdmin_returnsSimilarityPairs() throws Exception {

    SubmissionSimilarityResponse resp =
        new SubmissionSimilarityResponse(
            1L,
            (short) 1,
            10L,
            11L,
            UUID.randomUUID(),
            "Team A",
            UUID.randomUUID(),
            "Team B",
            new java.math.BigDecimal("0.70"),
            new java.math.BigDecimal("0.50"),
            new java.math.BigDecimal("0.62"),
            8,
            true,
            Instant.now()
        );
    
    when(checkService.getResults(EVENT_ID, (short) 1, false)).thenReturn(List.of(resp));

    mockMvc
        .perform(
            get("/api/admin/events/{eventId}/plagiarism/pairs", EVENT_ID)
                .param("levelId", "1")
                .with(authentication(adminAuth))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].submissionIdA").value(10))
        .andExpect(jsonPath("$[0].flagged").value(true));
  }

  @Test
  void getPairs_asParticipant_returns403() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/events/{eventId}/plagiarism/pairs", EVENT_ID)
                .with(authentication(participantAuth))
        )
        .andExpect(status().isForbidden());
  }


}