package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.CodeWorkspaceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CodeWorkspaceServiceTest {
  @Mock private CodeWorkspaceRepository workspaceRepo;
  @Mock private CodeWorkspaceAccessService accService;
  private CodeWorkspaceService service;

  private final UUID eventId = UUID.randomUUID();
  private final UUID teamId = UUID.randomUUID();
  private final UUID hackathonId = UUID.randomUUID();
  private final short levelId = 1;
  private final User user = User.builder().userId(UUID.randomUUID()).build();

  @BeforeEach
  void setUp() {
    service = new CodeWorkspaceService(workspaceRepo, accService);
  }

  private CodeWorkspace workspaceWith(UUID workspaceHackathonId) {
    CodeWorkspace workspace = mock(CodeWorkspace.class);
    when(workspace.getEventId()).thenReturn(eventId);
    when(workspace.getTeamId()).thenReturn(teamId);
    when(workspace.getLevelId()).thenReturn(levelId);
    when(workspace.getHackathonId()).thenReturn(workspaceHackathonId);
    return workspace;
  }

  @Test
  void getOrCreateWorkspace_createsIfMissingAndReturnsWorkspace() {
    CodeWorkspace workspace =
        new CodeWorkspace(eventId, teamId, hackathonId, levelId, user.getUserId());
    when(accService.requireParticipantAccess(eventId, teamId, levelId, user.getUserId()))
        .thenReturn(hackathonId);
    when(workspaceRepo.findByEventIdAndTeamIdAndLevelId(eventId, teamId, levelId))
        .thenReturn(Optional.of(workspace));
    CodeWorkspace res = service.getOrCreateWorkspace(eventId, teamId, levelId, user);

    assertThat(res).isSameAs(workspace);
    verify(workspaceRepo).createIfMissing(eventId, teamId, hackathonId, levelId, user.getUserId());
  }

  @Test
  void getOrCreateWorkspace_throws_whenUserNull() {
    assertThatThrownBy(() -> service.getOrCreateWorkspace(eventId, teamId, levelId, null))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getOrCreateWorkspace_throws_whenUserHasNoId() {
    User noId = User.builder().build();

    assertThatThrownBy(() -> service.getOrCreateWorkspace(eventId, teamId, levelId, noId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getOrCreateWorkspace_throws_whenEventOrTeamIdMissing() {
    assertThatThrownBy(() -> service.getOrCreateWorkspace(null, teamId, levelId, user))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.getOrCreateWorkspace(eventId, null, levelId, user))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getOrCreateWorkspace_throws_whenWorkspaceCannotBeLoaded() {
    when(accService.requireParticipantAccess(eventId, teamId, levelId, user.getUserId()))
        .thenReturn(hackathonId);
    when(workspaceRepo.findByEventIdAndTeamIdAndLevelId(eventId, teamId, levelId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getOrCreateWorkspace(eventId, teamId, levelId, user))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void getWorkspaceForUser_returnsWorkspace_whenUserHasAccess() {
    UUID workspaceId = UUID.randomUUID();
    CodeWorkspace workspace = workspaceWith(hackathonId);
    when(workspaceRepo.findById(workspaceId)).thenReturn(Optional.of(workspace));
    when(accService.requireParticipantAccess(eventId, teamId, levelId, user.getUserId()))
        .thenReturn(hackathonId);

    assertThat(service.getWorkspaceForUser(workspaceId, user)).isSameAs(workspace);
  }

  @Test
  void getWorkspaceForUser_throws_whenUserNull() {
    assertThatThrownBy(() -> service.getWorkspaceForUser(UUID.randomUUID(), null))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getWorkspaceForUser_throws_whenWorkspaceIdNull() {
    assertThatThrownBy(() -> service.getWorkspaceForUser(null, user))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getWorkspaceForUser_throws_whenWorkspaceNotFound() {
    UUID workspaceId = UUID.randomUUID();
    when(workspaceRepo.findById(workspaceId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getWorkspaceForUser(workspaceId, user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Could not find");
  }

  @Test
  void getWorkspaceForUser_throws_whenHackathonDoesNotMatch() {
    UUID workspaceId = UUID.randomUUID();
    CodeWorkspace workspace = workspaceWith(hackathonId);
    when(workspaceRepo.findById(workspaceId)).thenReturn(Optional.of(workspace));
    when(accService.requireParticipantAccess(eventId, teamId, levelId, user.getUserId()))
        .thenReturn(UUID.randomUUID());

    assertThatThrownBy(() -> service.getWorkspaceForUser(workspaceId, user))
        .isInstanceOf(AccessDeniedException.class);
  }
}
