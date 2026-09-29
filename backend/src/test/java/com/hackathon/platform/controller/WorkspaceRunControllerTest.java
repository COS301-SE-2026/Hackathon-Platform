package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.WorkspaceRunResponse;
import com.hackathon.platform.ide.WorkspaceRunService;
import com.hackathon.platform.model.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceRunControllerTest {
  @Mock private WorkspaceRunService runService;
  @InjectMocks private WorkspaceRunController controller;

  @Test
  void runWorkspace_returnsRunResult() {
    UUID workspaceId = UUID.randomUUID();
    User user = User.builder().userId(UUID.randomUUID()).build();
    WorkspaceRunResponse result = new WorkspaceRunResponse(true, 0, "{}", "");
    when(runService.runWorkspace(workspaceId, user)).thenReturn(result);

    assertThat(controller.runWorkspace(workspaceId, user)).isEqualTo(result);
  }
}
