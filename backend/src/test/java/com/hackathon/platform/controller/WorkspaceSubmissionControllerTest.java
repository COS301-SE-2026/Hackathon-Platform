package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hackathon.platform.ide.WorkspaceSubmissionService;
import com.hackathon.platform.model.User;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceSubmissionControllerTest {
  @Mock private WorkspaceSubmissionService subService;
  @InjectMocks private WorkspaceSubmissionController controller;

  @Test
  void submitWorkspace_returnsSubmissionInfo() {
    UUID workspaceId = UUID.randomUUID();
    User user = User.builder().userId(UUID.randomUUID()).build();
    when(subService.createSubmission(workspaceId, user)).thenReturn(Map.of("submissionId", "abc"));

    var res = controller.submitWorkspace(workspaceId, user);

    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat(res.getBody()).containsEntry("submissionId", "abc");
  }
}
