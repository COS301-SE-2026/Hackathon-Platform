package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.platform.model.Level;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.TeamMember;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.LevelRepository;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CodeWorkspaceAccessServiceTest {
  @Mock private EventRepository eventRepo;
  @Mock private LevelRepository lvlRepo;
  @Mock private TeamMemberRepository teamMRepo;
  @Mock private TeamRepository teamRepo;
  private CodeWorkspaceAccessService service;

  private final UUID eventId = UUID.randomUUID();
  private final UUID teamId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID hackathonId = UUID.randomUUID();
  private final short levelId = 1;

  @BeforeEach
  void setUp() {
    service = new CodeWorkspaceAccessService(eventRepo, lvlRepo, teamRepo, teamMRepo);
  }

  private void givenTeamInEvent() {
    Team team = mock(Team.class);
    when(team.getEventId()).thenReturn(eventId);
    when(teamRepo.findById(teamId)).thenReturn(Optional.of(team));
  }

  private void givenApprovedMember() {
    TeamMember member = mock(TeamMember.class);
    when(member.getTeamId()).thenReturn(teamId);
    when(teamMRepo.findByUserIdAndStatusAndEventId(userId, "APPROVED", eventId))
        .thenReturn(List.of(member));
  }

  private Level givenLevelInHackathon(UUID hackId) {
    Level level = mock(Level.class);
    when(level.getHackathonId()).thenReturn(hackId);
    when(lvlRepo.findById(levelId)).thenReturn(Optional.of(level));
    return level;
  }

  @Test
  void requireParticipantAccess_returnsHackathonId_whenEverythingMatches() {
    givenTeamInEvent();
    givenApprovedMember();
    givenLevelInHackathon(hackathonId);
    when(eventRepo.findHackathonIdByEventId(eventId)).thenReturn(Optional.of(hackathonId));
    assertThat(service.requireParticipantAccess(eventId, teamId, levelId, userId))
        .isEqualTo(hackathonId);
  }

  @Test
  void requireParticipantAccess_throws_whenTeamNotFound() {
    when(teamRepo.findById(teamId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.requireParticipantAccess(eventId, teamId, levelId, userId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Team not found");
  }

  @Test
  void requireParticipantAccess_throws_whenTeamBelongsToAnotherEvent() {
    Team team = mock(Team.class);
    when(team.getEventId()).thenReturn(UUID.randomUUID());
    when(teamRepo.findById(teamId)).thenReturn(Optional.of(team));
    assertThatThrownBy(() -> service.requireParticipantAccess(eventId, teamId, levelId, userId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void requireParticipantAccess_throws_whenUserIsNotAnApprovedMember() {
    givenTeamInEvent();
    when(teamMRepo.findByUserIdAndStatusAndEventId(userId, "APPROVED", eventId))
        .thenReturn(List.of());
    assertThatThrownBy(() -> service.requireParticipantAccess(eventId, teamId, levelId, userId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void requireParticipantAccess_throws_whenLevelNotFound() {
    givenTeamInEvent();
    givenApprovedMember();
    when(lvlRepo.findById(levelId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.requireParticipantAccess(eventId, teamId, levelId, userId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void requireParticipantAccess_throws_whenHackathonNotFound() {
    givenTeamInEvent();
    givenApprovedMember();
    when(lvlRepo.findById(levelId)).thenReturn(Optional.of(mock(Level.class)));
    when(eventRepo.findHackathonIdByEventId(eventId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.requireParticipantAccess(eventId, teamId, levelId, userId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void requireParticipantAccess_throws_whenLevelBelongsToAnotherHackaton() {
    givenTeamInEvent();
    givenApprovedMember();
    givenLevelInHackathon(UUID.randomUUID());
    when(eventRepo.findHackathonIdByEventId(eventId)).thenReturn(Optional.of(hackathonId));
    assertThatThrownBy(() -> service.requireParticipantAccess(eventId, teamId, levelId, userId))
        .isInstanceOf(AccessDeniedException.class);
  }
}
