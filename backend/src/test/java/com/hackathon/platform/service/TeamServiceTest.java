package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.CreateTeamRequest;
import com.hackathon.platform.dto.TeamResponse;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.TeamMember;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.repository.UserRepository;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private TeamService teamService;

    private UUID eventId;
    private UUID userId;
    private CreateTeamRequest createRequest;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();
        createRequest = new CreateTeamRequest();
        createRequest.setEventId(eventId);
        createRequest.setTeamName("Test Team");
    }

    @Test
    void createTeam_shouldSucceed_whenValid() {
        
        when(teamRepository.existsByEventIdAndTeamName(eventId, "Test Team")).thenReturn(false);
        when(teamMemberRepository.findByUserIdAndStatus(userId, "APPROVED")).thenReturn(Collections.emptyList());

        Team savedTeam = new Team();
        savedTeam.setTeamId(UUID.randomUUID());
        savedTeam.setTeamName("Test Team");
        savedTeam.setEventId(eventId);
        savedTeam.setCreatedByUserId(userId);
        when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        TeamResponse response = teamService.createTeam(createRequest, userId);

        assertThat(response.getTeamName()).isEqualTo("Test Team");
        assertThat(response.getEventId()).isEqualTo(eventId);
        assertThat(response.getCreatedByUserId()).isEqualTo(userId);
        verify(teamRepository).save(any(Team.class));
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    void createTeam_shouldThrow_whenDuplicateTeamName() {
        
        when(teamRepository.existsByEventIdAndTeamName(eventId, "Test Team")).thenReturn(true);
        assertThatThrownBy(() -> teamService.createTeam(createRequest, userId)).isInstanceOf(RuntimeException.class).hasMessageContaining("Team name already exists in this event");
        verify(teamRepository, never()).save(any(Team.class));
        verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    void createTeam_shouldThrow_whenUserAlreadyInTeamForEvent() {
        
        when(teamRepository.existsByEventIdAndTeamName(eventId, "Test Team")).thenReturn(false);

        Team existingTeam = new Team();
        existingTeam.setEventId(eventId);
        TeamMember existingMember = new TeamMember();
        existingMember.setTeamId(UUID.randomUUID());
        when(teamMemberRepository.findByUserIdAndStatus(userId, "APPROVED")).thenReturn(List.of(existingMember));
        when(teamRepository.findById(existingMember.getTeamId())).thenReturn(Optional.of(existingTeam));

        assertThatThrownBy(() -> teamService.createTeam(createRequest, userId)).isInstanceOf(RuntimeException.class).hasMessageContaining("already a member of a team in this event");
        verify(teamRepository, never()).save(any(Team.class));
        verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    void requestToJoinTeam_shouldSucceed_whenValid() {
        UUID teamId = UUID.randomUUID();
        Team team = new Team();
        team.setEventId(eventId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());
        when(teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED")).thenReturn(0L);
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        teamService.requestToJoinTeam(teamId, userId);

        verify(teamMemberRepository).save(any(TeamMember.class));
    }

}