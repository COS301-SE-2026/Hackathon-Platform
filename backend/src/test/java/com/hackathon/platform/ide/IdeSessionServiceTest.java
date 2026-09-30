package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.IdeSessionResponse;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class IdeSessionServiceTest {
  @Mock private CodeWorkspaceService workService;
  @Mock private IdeContainerManager containerManager;
  @Mock private IdeAccessManager accessManager;
  @Mock private WorkspaceInitializationService initService;
  private IdeSessionService service;

  @BeforeEach
  void setUp() {
    service = new IdeSessionService(workService, containerManager, accessManager, initService);
  }

  @Test
  void startOrReuseSession_startsContainerInitializesAndReturnsUrl() {
    UUID workspaceId = UUID.randomUUID();
    User user = User.builder().userId(UUID.randomUUID()).build();
    CodeWorkspace workspace = mock(CodeWorkspace.class);
    IdeWorkspaceResources resources = IdeWorkspaceResources.forWorkspace(workspaceId);
    IdeContainerSession session = new IdeContainerSession(workspaceId, 8080, "RUNNING");

    when(workspace.getWorkspaceId()).thenReturn(workspaceId);
    when(workService.getWorkspaceForUser(workspaceId, user)).thenReturn(workspace);
    when(containerManager.startOrReuse(resources)).thenReturn(session);
    when(accessManager.getIdeUrl(session)).thenReturn("http://localhost:8080");

    IdeSessionResponse res = service.startOrReuseSession(workspaceId, user);

    assertThat(res.workspaceId()).isEqualTo(workspaceId);
    assertThat(res.ideUrl()).isEqualTo("http://localhost:8080");
    assertThat(res.status()).isEqualTo("RUNNING");
    verify(initService).initializeIfNeeded(workspaceId, resources);
  }

  @Test
  void startOrReuseSession_throws_whenUserHasNoAccess() {
    UUID workspaceId = UUID.randomUUID();
    User user = User.builder().userId(UUID.randomUUID()).build();
    when(workService.getWorkspaceForUser(workspaceId, user))
        .thenThrow(new AccessDeniedException("nope"));

    assertThatThrownBy(() -> service.startOrReuseSession(workspaceId, user))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(containerManager, initService, accessManager);
  }
}
