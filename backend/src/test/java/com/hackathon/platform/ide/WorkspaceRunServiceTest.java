package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.WorkspaceRunResponse;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceRunServiceTest {
  @Mock private CodeWorkspaceService workService;
  @Mock private IdeContainerManager containerManager;
  @Mock private WorkspaceInitializationService initService;
  @Mock private WorkspaceCodeRunner codeRunner;

  @Test
  void runWorkspace_startsContainerInitializesAndRunsCode() {
    WorkspaceRunService service =
        new WorkspaceRunService(workService, containerManager, initService, codeRunner);
    UUID workspaceId = UUID.randomUUID();
    User user = User.builder().userId(UUID.randomUUID()).build();
    CodeWorkspace workspace = mock(CodeWorkspace.class);
    IdeWorkspaceResources resources = IdeWorkspaceResources.forWorkspace(workspaceId);
    WorkspaceRunResponse runResponse = new WorkspaceRunResponse(true, 0, "{}", "");

    when(workspace.getWorkspaceId()).thenReturn(workspaceId);
    when(workService.getWorkspaceForUser(workspaceId, user)).thenReturn(workspace);
    when(codeRunner.run(resources)).thenReturn(runResponse);

    WorkspaceRunResponse res = service.runWorkspace(workspaceId, user);

    assertThat(res).isEqualTo(runResponse);
    verify(containerManager).startOrReuse(resources);
    verify(initService).initializeIfNeeded(workspaceId, resources);
  }
}
