package com.hackathon.platform.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.platform.dto.LeaderboardEntryResponse;
import com.hackathon.platform.dto.ScoringLogResponse;
import com.hackathon.platform.dto.SubmissionResponse;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.UserRepository;
import com.hackathon.platform.scoring.LeaderboardService;
import com.hackathon.platform.scoring.LeaderboardUpdateService;
import com.hackathon.platform.scoring.ScoringService;
import com.hackathon.platform.scoring.SubmissionQueryService;
import com.hackathon.platform.scoring.queue.ScoringJobProducer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ScoringControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ScoringService scoringService;

  @MockBean private SubmissionQueryService submissionQueryService;

  @MockBean private LeaderboardService leaderboardService;

  @MockBean private ScoringJobProducer scoringJobProducer;

  @MockBean private LeaderboardUpdateService leaderboardUpdateService;

  @MockBean private UserRepository userRepository;

  private static final UUID TEAM_ID = UUID.fromString("d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14");
  private static final UUID EVENT_ID = UUID.fromString("c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14");
  private static final UUID USER_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
  private static final Long SUBMISSION_ID = 1L;
  private static final Long LEVEL_ID = 2L;
  private static final int LIMIT = 5;
  private static final String RECORD_ID = "record-123";

  private UsernamePasswordAuthenticationToken adminAuth;
  private UsernamePasswordAuthenticationToken participantAuth;
  private User adminUser;
  private User participantUser;

  @BeforeEach
  void setUp() {
    // Create admin user
    adminUser =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("Admin")
            .lastName("User")
            .email("admin@test.com")
            .passwordHash("hash")
            .status("ACTIVE")
            .role(Role.builder().roleId(1).name("ADMIN").build())
            .build();

    // Create participant user
    participantUser =
        User.builder()
            .userId(TEAM_ID)
            .firstName("Participant")
            .lastName("User")
            .email("participant@test.com")
            .passwordHash("hash")
            .status("ACTIVE")
            .role(Role.builder().roleId(2).name("PARTICIPANT").build())
            .build();

    // Create authentication tokens
    adminAuth =
        new UsernamePasswordAuthenticationToken(
            adminUser, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    participantAuth =
        new UsernamePasswordAuthenticationToken(
            participantUser, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
  }

  @Test
  void scoreSubmission_returns202WithQueuedStatus() throws Exception {
    when(scoringJobProducer.enqueue(anyLong())).thenReturn(RECORD_ID);

    mockMvc
        .perform(
            post("/api/scoring/submissions/{submissionId}/score", SUBMISSION_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.submissionId").value(SUBMISSION_ID))
        .andExpect(jsonPath("$.status").value("QUEUED"))
        .andExpect(jsonPath("$.recordId").value(RECORD_ID));
  }

  @Test
  void scoreSubmission_returns202WithEmptyRecordIdWhenNoRecord() throws Exception {
    when(scoringJobProducer.enqueue(anyLong())).thenReturn(null);

    mockMvc
        .perform(
            post("/api/scoring/submissions/{submissionId}/score", SUBMISSION_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.submissionId").value(SUBMISSION_ID))
        .andExpect(jsonPath("$.status").value("QUEUED"))
        .andExpect(jsonPath("$.recordId").value(""));
  }

  @Test
  void getTeamHistory_returnsSubmissionListWithoutLogs() throws Exception {
    SubmissionResponse response =
        new SubmissionResponse(
            SUBMISSION_ID,
            TEAM_ID,
            LEVEL_ID,
            3L,
            new BigDecimal("74.55"),
            "SCORED",
            Instant.now(),
            "output.txt",
            "code.zip",
            null);

    when(submissionQueryService.getHistoryForTeam(TEAM_ID)).thenReturn(List.of(response));

    mockMvc
        .perform(
            get("/api/scoring/teams/{teamId}/submissions", TEAM_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].submissionId").value(SUBMISSION_ID))
        .andExpect(jsonPath("$[0].status").value("SCORED"))
        .andExpect(jsonPath("$[0].score").value(74.55));
  }

  @Test
  void getTeamLevelHistory_returnsScopedSubmissionList() throws Exception {
    SubmissionResponse response =
        new SubmissionResponse(
            SUBMISSION_ID,
            TEAM_ID,
            LEVEL_ID,
            3L,
            new BigDecimal("85.5"),
            "SCORED",
            Instant.now(),
            "output.txt",
            "code.zip",
            null);

    when(submissionQueryService.getHistoryForTeamAndLevel(TEAM_ID, LEVEL_ID))
        .thenReturn(List.of(response));

    mockMvc
        .perform(
            get("/api/scoring/teams/{teamId}/levels/{levelId}/submissions", TEAM_ID, LEVEL_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].submissionId").value(SUBMISSION_ID))
        .andExpect(jsonPath("$[0].levelId").value(LEVEL_ID));
  }

  @Test
  void getRecentSubmissions_returnRecentSubmissions() throws Exception {
    SubmissionResponse response =
        new SubmissionResponse(
            SUBMISSION_ID,
            TEAM_ID,
            LEVEL_ID,
            3L,
            new BigDecimal("92.0"),
            "SCORED",
            Instant.now(),
            "output.txt",
            "code.zip",
            null);

    when(submissionQueryService.getRecentSubmissions(any(), anyInt()))
        .thenReturn(List.of(response));

    mockMvc
        .perform(
            get("/api/scoring/admin/recentsubmissions/{limit}", LIMIT)
                .with(authentication(adminAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].submissionId").value(SUBMISSION_ID));
  }

  @Test
  void getRecentSubmissions_returns403WhenCallerIsNotAdmin() throws Exception {
    mockMvc
        .perform(
            get("/api/scoring/admin/recentsubmissions/{limit}", LIMIT)
                .with(authentication(participantAuth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getSubmissionDetail_returnsFullFeedbackWithLogs() throws Exception {
    ScoringLogResponse log =
        new ScoringLogResponse(
            TEAM_ID,
            EVENT_ID,
            "logs/event/team/level/scoring_log.txt",
            1,
            Instant.now(),
            "malformed output on row 2 - MALFORMED_OUTPUT");

    SubmissionResponse response =
        new SubmissionResponse(
            SUBMISSION_ID,
            TEAM_ID,
            LEVEL_ID,
            3L,
            BigDecimal.ZERO,
            "FAILED",
            Instant.now(),
            "output.txt",
            "code.zip",
            log);

    when(submissionQueryService.getSubmissionDetail(SUBMISSION_ID, TEAM_ID)).thenReturn(response);

    mockMvc
        .perform(
            get("/api/scoring/teams/{teamId}/submissions/{submissionId}", TEAM_ID, SUBMISSION_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submissionId").value(SUBMISSION_ID))
        .andExpect(jsonPath("$.status").value("FAILED"))
        .andExpect(
            jsonPath("$.scoringLog.logContent")
                .value("malformed output on row 2 - MALFORMED_OUTPUT"));
  }

  @Test
  void getSubmissionDetailForAdmin_returnsFeedbackForAnyTeam() throws Exception {
    SubmissionResponse response =
        new SubmissionResponse(
            SUBMISSION_ID,
            TEAM_ID,
            LEVEL_ID,
            3L,
            new BigDecimal("74.55"),
            "SCORED",
            Instant.now(),
            "output.txt",
            "code.zip",
            null);

    when(submissionQueryService.getSubmissionDetailForAdmin(SUBMISSION_ID)).thenReturn(response);

    mockMvc
        .perform(
            get("/api/scoring/admin/submissions/{submissionId}", SUBMISSION_ID)
                .with(authentication(adminAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submissionId").value(SUBMISSION_ID))
        .andExpect(jsonPath("$.status").value("SCORED"));
  }

  @Test
  void getSubmissionDetailForAdmin_returns403WhenCallerIsNotAdmin() throws Exception {
    mockMvc
        .perform(
            get("/api/scoring/admin/submissions/{submissionId}", SUBMISSION_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getLeaderboard_returnsLeaderboardForLevel() throws Exception {
    LeaderboardEntryResponse entry =
        new LeaderboardEntryResponse(
            1, // rank
            TEAM_ID,
            "Team Alpha",
            new BigDecimal("95.5"),
            Instant.now());

    when(leaderboardService.getLeaderboard(EVENT_ID, LEVEL_ID)).thenReturn(List.of(entry));

    mockMvc
        .perform(
            get("/api/scoring/events/{eventId}/levels/{levelId}/leaderboard", EVENT_ID, LEVEL_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].rank").value(1))
        .andExpect(jsonPath("$[0].teamId").value(TEAM_ID.toString()))
        .andExpect(jsonPath("$[0].teamName").value("Team Alpha"))
        .andExpect(jsonPath("$[0].bestScore").value(95.5))
        .andExpect(jsonPath("$[0].lastScoredAt").exists());
  }

  @Test
  void getEventLeaderboard_returnsLeaderboardForEvent() throws Exception {
    LeaderboardEntryResponse entry =
        new LeaderboardEntryResponse(
            1, // rank
            TEAM_ID,
            "Team Alpha",
            new BigDecimal("185.5"),
            Instant.now());

    when(leaderboardService.getEventLeaderboard(EVENT_ID)).thenReturn(List.of(entry));

    mockMvc
        .perform(
            get("/api/scoring/events/{eventId}/leaderboard", EVENT_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].rank").value(1))
        .andExpect(jsonPath("$[0].teamId").value(TEAM_ID.toString()))
        .andExpect(jsonPath("$[0].teamName").value("Team Alpha"))
        .andExpect(jsonPath("$[0].bestScore").value(185.5))
        .andExpect(jsonPath("$[0].lastScoredAt").exists());
  }

  @Test
  void scoreSubmission_returns403WhenCallerIsNotAuthenticated() throws Exception {
    // When no authentication is provided, Spring Security returns 403 (Forbidden)
    mockMvc
        .perform(post("/api/scoring/submissions/{submissionId}/score", SUBMISSION_ID))
        .andExpect(status().isForbidden());
  }

  @Test
  void getTeamHistory_returns403WhenCallerIsNotAuthenticated() throws Exception {
    mockMvc
        .perform(get("/api/scoring/teams/{teamId}/submissions", TEAM_ID))
        .andExpect(status().isForbidden());
  }

  @Test
  void getLeaderboard_returns403WhenCallerIsNotAuthenticated() throws Exception {
    mockMvc
        .perform(
            get("/api/scoring/events/{eventId}/levels/{levelId}/leaderboard", EVENT_ID, LEVEL_ID))
        .andExpect(status().isForbidden());
  }
}
