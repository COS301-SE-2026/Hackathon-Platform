package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.CreateTeamRequest;
import com.hackathon.platform.dto.TeamMemberResponse;
import com.hackathon.platform.dto.TeamResponse;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.TeamMember;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.repository.UserRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    when(teamMemberRepository.findByUserIdAndStatus(userId, "APPROVED"))
        .thenReturn(Collections.emptyList());

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
    assertThatThrownBy(() -> teamService.createTeam(createRequest, userId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Team name already exists in this event");
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
    when(teamMemberRepository.findByUserIdAndStatus(userId, "APPROVED"))
        .thenReturn(List.of(existingMember));
    when(teamRepository.findById(existingMember.getTeamId())).thenReturn(Optional.of(existingTeam));

    assertThatThrownBy(() -> teamService.createTeam(createRequest, userId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("already a member of a team in this event");
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

  @Test
  void requestToJoinTeam_shouldThrow_whenAlreadyRequested() {
    UUID teamId = UUID.randomUUID();
    when(teamRepository.findById(teamId)).thenReturn(Optional.of(new Team()));
    when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
        .thenReturn(Optional.of(new TeamMember()));

    assertThatThrownBy(() -> teamService.requestToJoinTeam(teamId, userId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Already requested or member");
    verify(teamMemberRepository, never()).save(any(TeamMember.class));
  }

  @Test
  void requestToJoinTeam_shouldThrow_whenTeamFull() {
    UUID teamId = UUID.randomUUID();
    Team team = new Team();
    team.setEventId(eventId);
    when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
    when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());
    when(teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED")).thenReturn(4L);

    assertThatThrownBy(() -> teamService.requestToJoinTeam(teamId, userId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Team is full");

    verify(teamMemberRepository, never()).save(any(TeamMember.class));
  }

  @Test
  void approveOrRejectJoinRequest_shouldApprove_whenValid() {
    UUID teamId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    Team team = new Team();
    team.setEventId(eventId);
    team.setCreatedByUserId(userId);
    TeamMember pending = new TeamMember();
    pending.setStatus("PENDING");

    when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
    when(teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId))
        .thenReturn(Optional.of(pending));
    when(teamMemberRepository.findByUserIdAndStatus(targetUserId, "APPROVED"))
        .thenReturn(Collections.emptyList());
    when(teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED")).thenReturn(1L);

    teamService.approveOrRejectJoinRequest(teamId, targetUserId, userId, true);

    assertThat(pending.getStatus()).isEqualTo("APPROVED");
    verify(teamMemberRepository).save(pending);
  }

  @Test
  void approveOrRejectJoinRequest_shouldReject_whenValid() {
    UUID teamId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    Team team = new Team();
    team.setCreatedByUserId(userId);
    TeamMember pending = new TeamMember();
    pending.setStatus("PENDING");

    when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
    when(teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId))
        .thenReturn(Optional.of(pending));

    teamService.approveOrRejectJoinRequest(teamId, targetUserId, userId, false);

    assertThat(pending.getStatus()).isEqualTo("REJECTED");
    verify(teamMemberRepository).save(pending);
  }

  @Test
  void approveOrRejectJoinRequest_shouldThrow_whenUserAlreadyApprovedInAnotherTeam() {

    UUID targetTeamId = UUID.randomUUID();
    UUID otherTeamId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    Team targetTeam = new Team();
    targetTeam.setEventId(eventId);
    targetTeam.setCreatedByUserId(userId);
    Team otherTeam = new Team();
    otherTeam.setEventId(eventId);
    TeamMember pendingMembership = new TeamMember();
    pendingMembership.setStatus("PENDING");
    TeamMember approvedMembershipInOtherTeam = new TeamMember();
    approvedMembershipInOtherTeam.setTeamId(otherTeamId);
    approvedMembershipInOtherTeam.setStatus("APPROVED");

    when(teamRepository.findById(targetTeamId)).thenReturn(Optional.of(targetTeam));
    when(teamMemberRepository.findByTeamIdAndUserId(targetTeamId, targetUserId))
        .thenReturn(Optional.of(pendingMembership));
    when(teamMemberRepository.findByUserIdAndStatus(targetUserId, "APPROVED"))
        .thenReturn(List.of(approvedMembershipInOtherTeam));
    when(teamRepository.findById(otherTeamId)).thenReturn(Optional.of(otherTeam));

    assertThatThrownBy(
            () -> teamService.approveOrRejectJoinRequest(targetTeamId, targetUserId, userId, true))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("already an approved member of another team in this event");

    verify(teamMemberRepository, never()).save(pendingMembership);
  }

  @Test
  void approveOrRejectJoinRequest_shouldThrow_whenRequestAlreadyProcessed() {
    UUID teamId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    Team team = new Team();
    team.setCreatedByUserId(userId);
    TeamMember pending = new TeamMember();
    pending.setStatus("APPROVED");

    when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
    when(teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId))
        .thenReturn(Optional.of(pending));

    assertThatThrownBy(
            () -> teamService.approveOrRejectJoinRequest(teamId, targetUserId, userId, true))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Request already processed");
    verify(teamMemberRepository, never()).save(pending);
  }

  @Test
  void approveOrRejectJoinRequest_shouldThrow_whenNotCreator() {
    UUID teamId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    UUID nonCreator = UUID.randomUUID();
    Team team = new Team();
    team.setCreatedByUserId(UUID.randomUUID());

    when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

    assertThatThrownBy(
            () -> teamService.approveOrRejectJoinRequest(teamId, targetUserId, nonCreator, true))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Only the team creator can approve/reject requests");

    verify(teamMemberRepository, never()).save(any(TeamMember.class));
  }

  @Test
  void leaveTeam_shouldSetStatusToLeft_whenApprovedMember() {
    UUID teamId = UUID.randomUUID();
    TeamMember membership = new TeamMember();
    membership.setStatus("APPROVED");
    when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
        .thenReturn(Optional.of(membership));
    when(teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED")).thenReturn(1L);

    teamService.leaveTeam(teamId, userId);

    assertThat(membership.getStatus()).isEqualTo("LEFT");
    verify(teamMemberRepository).save(membership);
    verify(teamRepository, never()).save(any());
  }

  @Test
  void leaveTeam_shouldSetTeamInactive_whenLastMemberLeaves() {
    UUID teamId = UUID.randomUUID();
    TeamMember membership = new TeamMember();
    membership.setStatus("APPROVED");
    Team team = new Team();
    team.setStatus("ACTIVE");
    when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
        .thenReturn(Optional.of(membership));
    when(teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED")).thenReturn(0L);
    when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
    when(teamRepository.save(any(Team.class))).thenReturn(team);
    teamService.leaveTeam(teamId, userId);
    assertThat(membership.getStatus()).isEqualTo("LEFT");
    assertThat(team.getStatus()).isEqualTo("INACTIVE");
    verify(teamMemberRepository).save(membership);
    verify(teamRepository).save(team);
  }

  @Test
  void leaveTeam_shouldDelete_whenPendingMember() {
    UUID teamId = UUID.randomUUID();
    TeamMember membership = new TeamMember();
    membership.setStatus("PENDING");
    when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
        .thenReturn(Optional.of(membership));
    teamService.leaveTeam(teamId, userId);
    verify(teamMemberRepository).delete(membership);
    verify(teamMemberRepository, never()).save(any());
  }

  @Test
  void viewTeamMembers_shouldReturnListOfMembers() {

    UUID teamId = UUID.randomUUID();
    UUID creatorId = userId;
    UUID memberId = UUID.randomUUID();
    Team team = new Team();
    team.setCreatedByUserId(creatorId);
    TeamMember approvedMember = new TeamMember();
    approvedMember.setUserId(memberId);
    approvedMember.setStatus("APPROVED");
    approvedMember.setJoinedAt(Instant.now());
    User memberUser = new User();
    memberUser.setFirstName("John");
    memberUser.setLastName("Cena");
    memberUser.setEmail("john@test.com");
    when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
    when(teamMemberRepository.findByTeamIdAndStatus(teamId, "APPROVED"))
        .thenReturn(List.of(approvedMember));
    when(userRepository.findById(memberId)).thenReturn(Optional.of(memberUser));
    List<TeamMemberResponse> members = teamService.viewTeamMembers(teamId);
    assertThat(members).hasSize(1);
    TeamMemberResponse response = members.get(0);
    assertThat(response.getFullName()).isEqualTo("John Cena");
    assertThat(response.getEmail()).isEqualTo("john@test.com");
    assertThat(response.getRole()).isEqualTo("MEMBER");
  }

  @Test
  void viewTeamMembers_shouldThrow_whenTeamNotFound() {
    UUID teamId = UUID.randomUUID();
    when(teamRepository.findById(teamId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> teamService.viewTeamMembers(teamId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Team not found");
  }
}
