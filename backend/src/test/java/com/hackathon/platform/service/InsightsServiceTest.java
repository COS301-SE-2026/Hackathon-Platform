package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.AdminDashboardResponse;
import com.hackathon.platform.dto.EventInsightsResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {
    
    @Mock private EventRepository eventRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    @InjectMocks private InsightsService insightsService;

    private UUID adminUserId;
    private UUID eventId;
    
    @BeforeEach
    void setUp(){
        adminUserId = UUID.randomUUID();
        eventId = UUID.randomUUID();

    }

    private Event buildEvent(UUID id, String status) {
        Event event = new Event();
        event.setEventId(id);
        event.setStatus(status);
        return event;

    }

    private Team buildTeam(UUID id, UUID eventId) {
        Team team = new Team();
        team.setTeamId(id);
        team.setEventId(eventId);
        return team;

    }

    @Test
    void getAdminDashboard_withActiveAndInactiveEvents_countsOnlyActiveAsActive() {
        Event activeEvent = buildEvent(UUID.randomUUID(), "ACTIVE");
        Event inactiveEvent = buildEvent(UUID.randomUUID(), "INACTIVE");
        when(eventRepository.fetchAllByAdmin(adminUserId))
            .thenReturn(List.of(activeEvent,inactiveEvent));
        when(teamRepository.findByEventId(any())).thenReturn(Collections.emptyList());
        when(submissionRepository.countByAdminSince(eq(adminUserId), any(Instant.class)))
            .thenReturn(5L);
        
        AdminDashboardResponse response = insightsService.getAdminDashboard(adminUserId);

        assertThat(response.getTotalEvents()).isEqualTo(2);
        assertThat(response.getActiveEvents()).isEqualTo(1);
        assertThat(response.getSubmissionsToday()).isEqualTo(5L);

    }

    @Test
    void getAdminDashboard_withNoEvents_returnsZeroedStats() {
        when(eventRepository.fetchAllByAdmin(adminUserId)).thenReturn(Collections.emptyList());
        when(submissionRepository.countByAdminSince(eq(adminUserId), any(Instant.class)))
            .thenReturn(0L);

        AdminDashboardResponse response = insightsService.getAdminDashboard(adminUserId);

        assertThat(response.getTotalEvents()).isZero();
        assertThat(response.getActiveEvents()).isZero();
        assertThat(response.getTotalParticipants()).isZero();
        assertThat(response.getSubmissionsToday()).isZero();

    }

    @Test
    void getAdminDashboard_sumsApprovedParticipantsAcrossAllEvents() {

        Event event1 = buildEvent(UUID.randomUUID(), "ACTIVE");
        Event event2 = buildEvent(UUID.randomUUID(), "ACTIVE");
        when(eventRepository.fetchAllByAdmin(adminUserId)).thenReturn(List.of(event1, event2));

        Team team1 = buildTeam(UUID.randomUUID(), event1.getEventId());
        Team team2 = buildTeam(UUID.randomUUID(), event2.getEventId());
        when(teamRepository.findByEventId(event1.getEventId())).thenReturn(List.of(team1));
        when(teamRepository.findByEventId(event2.getEventId())).thenReturn(List.of(team2));
        when(teamMemberRepository.countByTeamIdInAndStatus(eq(List.of(team1.getTeamId())), eq("APPROVED")))
            .thenReturn(3L);
        when(teamMemberRepository.countByTeamIdInAndStatus(eq(List.of(team2.getTeamId())), eq("APPROVED")))
            .thenReturn(4L);
        when(submissionRepository.countByAdminSince(eq(adminUserId), any(Instant.class)))
            .thenReturn(0L);
        
        AdminDashboardResponse response = insightsService.getAdminDashboard(adminUserId);

        assertThat(response.getTotalParticipants()).isEqualTo(7L);
        
    }


}