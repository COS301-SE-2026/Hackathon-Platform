package com.hackathon.platform.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.platform.dto.ScoringLogResponse;
import com.hackathon.platform.dto.SubmissionResponse;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.UserRepository;
import com.hackathon.platform.scoring.SubmissionQueryService;
import com.hackathon.platform.scoring.ScoringService;
import com.hackathon.platform.shared.security.JwtAuthFilter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ScoringController.class)
@AutoConfigureMockMvc(addFilters = false)
class ScoringControllerTest {
    @Autowired private MockMvc mockMvc;

    @MockBean private ScoringService scoringS;
    @MockBean private SubmissionQueryService subQueryS;
    @MockBean private JwtAuthFilter jwtAuthF;
    @MockBean private UserRepository userR;
    @MockBean private PasswordEncoder passwordE;
    @MockBean private AuthenticationProvider authProvider;

    private static final UUID TEAM_ID = UUID.fromString("d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14");
    private static final Long SUB_ID = 1L;
    private static final Long LEVEL_ID = 2L;

    @Test
    void scoreSubmission_returns200WithUpdatedSubmission() throws Exception {
        Submission scored = new Submission(TEAM_ID, LEVEL_ID, 3L, "src/code.zip", "out/output.txt");
        scored.setId(SUB_ID);
        scored.setScore(new BigDecimal("75.2"));
        scored.setStatus("SCORED");

        when(scoringS.scoreSubmission(SUB_ID)).thenReturn(scored);

        mockMvc.perform(post("/api/scoring/submissions/{submissionId}/score", SUB_ID)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SCORED")).andExpect(jsonPath("$.score").value(75.2));
    }

    @Test
    void getTeamHistory_returnsSubmissionListWithoutLogs() throws Exception {
        SubmissionResponse response = new SubmissionResponse(
            SUB_ID,
            TEAM_ID,
            LEVEL_ID,
            3L,
            new BigDecimal("74.55"),
            "SCORED",
            Instant.now(),
            "output.txt",
            "code.zip",
            null
        );

        when(subQueryS.getHistoryForTeam(TEAM_ID)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/scoring/teams/{teamId}/submissions", TEAM_ID)).andExpect(status().isOk()).andExpect(jsonPath("$[0].submissionId").value(SUB_ID)).andExpect(jsonPath("$[0].status").value("SCORED"));
    }
}