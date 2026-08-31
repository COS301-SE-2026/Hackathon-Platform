// package com.hackathon.platform.controller;

// import static org.mockito.ArgumentMatchers.anyInt;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.when;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// import com.hackathon.platform.dto.AdminDashboardResponse;
// import com.hackathon.platform.dto.EventInsightsResponse;
// import com.hackathon.platform.dto.LevelScoreStats;
// import com.hackathon.platform.dto.SubmissionRateBucket;
// import com.hackathon.platform.model.Role;
// import com.hackathon.platform.model.User;
// import com.hackathon.platform.service.InsightsService;
// import java.math.BigDecimal;
// import java.time.Instant;
// import java.util.List;
// import java.util.Map;
// import java.util.UUID;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.transaction.annotation.Transactional;

// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
// @AutoConfigureMockMvc
// @Transactional
// class AdminInsightsControllerTest {

//   @Autowired private MockMvc mockMvc;

//   @MockBean private InsightsService insightsService;

//   private static final UUID EVENT_ID = UUID.fromString("c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14");
//   private static final UUID ADMIN_USER_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

//   private UsernamePasswordAuthenticationToken adminAuth;
//   private UsernamePasswordAuthenticationToken participantAuth;
//   private User adminUser;
//   private User participantUser;

//   @BeforeEach
//   void setUp() {
//     adminUser =
//         User.builder()
//             .userId(ADMIN_USER_ID)
//             .firstName("Admin")
//             .lastName("User")
//             .email("admin@test.com")
//             .passwordHash("hash")
//             .status("ACTIVE")
//             .role(Role.builder().roleId(1).name("ADMIN").build())
//             .build();

//     participantUser =
//         User.builder()
//             .userId(UUID.randomUUID())
//             .firstName("Participant")
//             .lastName("User")
//             .email("participant@test.com")
//             .passwordHash("hash")
//             .status("ACTIVE")
//             .role(Role.builder().roleId(2).name("PARTICIPANT").build())
//             .build();

//     adminAuth =
//         new UsernamePasswordAuthenticationToken(
//             adminUser, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

//     participantAuth =
//         new UsernamePasswordAuthenticationToken(
//             participantUser, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
//   }

//   private AdminDashboardResponse sampleDashboard() {
//     return new AdminDashboardResponse(2L, 3L, 25L, 4L);
//   }

//   private EventInsightsResponse sampleEventInsights() {
//     Map<String, Long> byStatus = Map.of("QUEUED", 1L, "SCORING", 1L, "SCORED", 7L, "FAILED", 3L);
//     List<SubmissionRateBucket> rate =
//         List.of(new SubmissionRateBucket(Instant.parse("2026-08-24T10:00:00Z"), 5L));
//     List<LevelScoreStats> distribution =
//         List.of(
//             new LevelScoreStats(
//                 (short) 1,
//                 "Level 1",
//                 7L,
//                 new BigDecimal("10.0"),
//                 new BigDecimal("100.0"),
//                 new BigDecimal("55.5")));

//     return new EventInsightsResponse(EVENT_ID, 4L, 12L, 12L, 3L, byStatus, 0.3, rate, distribution);
//   }

//   @Test
//   void getAdminDashboard_asAdmin_returns200WithBodyFromService() throws Exception {
//     when(insightsService.getAdminDashboard(ADMIN_USER_ID)).thenReturn(sampleDashboard());

//     mockMvc
//         .perform(get("/api/admin/dashboard").with(authentication(adminAuth)))
//         .andExpect(status().isOk())
//         .andExpect(jsonPath("$.activeEvents").value(2))
//         .andExpect(jsonPath("$.totalEvents").value(3))
//         .andExpect(jsonPath("$.totalParticipants").value(25))
//         .andExpect(jsonPath("$.submissionsToday").value(4));
//   }

//   @Test
//   void getAdminDashboard_asParticipant_returns403Forbidden() throws Exception {
//     mockMvc
//         .perform(get("/api/admin/dashboard").with(authentication(participantAuth)))
//         .andExpect(status().isForbidden());
//   }

//   @Test
//   void getEventInsights_asAdmin_returns200WithBodyFromService() throws Exception {
//     when(insightsService.getEventInsights(eq(EVENT_ID), anyInt()))
//         .thenReturn(sampleEventInsights());

//     mockMvc
//         .perform(get("/api/admin/events/{id}/insights", EVENT_ID).with(authentication(adminAuth)))
//         .andExpect(status().isOk())
//         .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
//         .andExpect(jsonPath("$.activeTeams").value(4))
//         .andExpect(jsonPath("$.approvedParticipants").value(12))
//         .andExpect(jsonPath("$.totalSubmissions").value(12))
//         .andExpect(jsonPath("$.submissionsLastHour").value(3))
//         .andExpect(jsonPath("$.submissionsByStatus.SCORED").value(7))
//         .andExpect(jsonPath("$.submissionsByStatus.FAILED").value(3))
//         .andExpect(jsonPath("$.errorRate").value(0.3))
//         .andExpect(jsonPath("$.submissionRate", org.hamcrest.Matchers.hasSize(1)))
//         .andExpect(jsonPath("$.scoreDistributionByLevel", org.hamcrest.Matchers.hasSize(1)))
//         .andExpect(jsonPath("$.scoreDistributionByLevel[0].levelName").value("Level 1"));
//   }

//   @Test
//   void getEventInsights_asParticipant_returns403Forbidden() throws Exception {
//     mockMvc
//         .perform(
//             get("/api/admin/events/{id}/insights", EVENT_ID).with(authentication(participantAuth)))
//         .andExpect(status().isForbidden());
//   }

//   @Test
//   void getEventInsights_withDefaultTrendWindow_passes60ToService() throws Exception {
//     when(insightsService.getEventInsights(eq(EVENT_ID), eq(60))).thenReturn(sampleEventInsights());

//     mockMvc
//         .perform(get("/api/admin/events/{id}/insights", EVENT_ID).with(authentication(adminAuth)))
//         .andExpect(status().isOk());
//   }

//   @Test
//   void getEventInsights_withCustomTrendWindow_passesParamToService() throws Exception {
//     when(insightsService.getEventInsights(eq(EVENT_ID), eq(15))).thenReturn(sampleEventInsights());

//     mockMvc
//         .perform(
//             get("/api/admin/events/{id}/insights", EVENT_ID)
//                 .param("trendWindowMinutes", "15")
//                 .with(authentication(adminAuth)))
//         .andExpect(status().isOk());
//   }
// }
