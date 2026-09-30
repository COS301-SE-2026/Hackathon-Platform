package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceCollaborationServiceTest {
  @Mock private CodeWorkspaceService codeService;
  @Mock private IdeContainerManager containerManager;
  @Mock private WorkspaceInitializationService initService;
  @Mock private WorkspaceFileStore fileStore;
  private WorkspaceCollaborationService service;

  private final UUID workspaceId = UUID.randomUUID();
  private final User user = User.builder().userId(UUID.randomUUID()).build();
  private final IdeWorkspaceResources resources = IdeWorkspaceResources.forWorkspace(workspaceId);

  @BeforeEach
  void setUp() {
    service =
        new WorkspaceCollaborationService(codeService, containerManager, initService, fileStore);
    CodeWorkspace workspace = mock(CodeWorkspace.class);
    when(workspace.getWorkspaceId()).thenReturn(workspaceId);
    when(codeService.getWorkspaceForUser(workspaceId, user)).thenReturn(workspace);
  }

  @Test
  void applyEdit_writesFileAndIncrementsVersion() {
    WorkspaceEditMessage msg = new WorkspaceEditMessage("Main.java", "class Main {}", 0);

    WorkspaceEditBroadcast res = service.applyEdit(workspaceId, msg, user);

    assertThat(res.path()).isEqualTo("Main.java");
    assertThat(res.content()).isEqualTo("class Main {}");
    assertThat(res.version()).isEqualTo(1);
    assertThat(res.editedByUserId()).isEqualTo(user.getUserId());
    verify(containerManager).startOrReuse(resources);
    verify(initService).initializeIfNeeded(workspaceId, resources);
    verify(fileStore).writeFile(resources, "Main.java", "class Main {}");
  }

  @Test
  void applyEdit_throws_whenBaseVersionIsStale() {
    WorkspaceEditMessage msg = new WorkspaceEditMessage("Main.java", "class Main {}", 5);

    assertThatThrownBy(() -> service.applyEdit(workspaceId, msg, user))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Stale");
    verify(fileStore, never()).writeFile(any(), any(), any());
  }

  @Test
  void getVersion_startsAtZeroAndFollowsEdits() {
    assertThat(service.getVersion(workspaceId, "Main.java", user)).isZero();

    service.applyEdit(workspaceId, new WorkspaceEditMessage("Main.java", "a", 0), user);

    assertThat(service.getVersion(workspaceId, "Main.java", user)).isEqualTo(1);
  }
}
