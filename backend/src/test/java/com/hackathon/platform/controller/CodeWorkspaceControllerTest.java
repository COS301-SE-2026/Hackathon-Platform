package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.CodeWorkspaceResponse;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CodeWorkspaceControllerTest {
  @Mock private CodeWorkspaceService workService;
  @InjectMocks private CodeWorkspaceController controller;

  @Test
  void getOrCreateWorkspace_returnsWorkspaceResponse() {
    UUID eventId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();
    UUID hackathonId = UUID.randomUUID();
    User user = User.builder().userId(UUID.randomUUID()).build();
    CodeWorkspace workspace =
        new CodeWorkspace(eventId, teamId, hackathonId, (short) 2, user.getUserId());
    when(workService.getOrCreateWorkspace(eventId, teamId, (short) 2, user)).thenReturn(workspace);

    ResponseEntity<CodeWorkspaceResponse> res =
        controller.getOrCreateWorkspace(eventId, teamId, (short) 2, user);

    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat(res.getBody().eventId()).isEqualTo(eventId);
    assertThat(res.getBody().teamId()).isEqualTo(teamId);
    assertThat(res.getBody().levelId()).isEqualTo((short) 2);
  }
}
