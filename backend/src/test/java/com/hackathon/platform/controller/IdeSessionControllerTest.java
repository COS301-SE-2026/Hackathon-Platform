package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.IdeSessionResponse;
import com.hackathon.platform.ide.IdeSessionService;
import com.hackathon.platform.model.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class IdeSessionControllerTest {
  @Mock private IdeSessionService ideService;
  @InjectMocks private IdeSessionController controller;

  @Test
  void startOrReuseIdeSession_returnsSessionFromService() {
    UUID workspaceId = UUID.randomUUID();
    User user = User.builder().userId(UUID.randomUUID()).build();
    IdeSessionResponse session = new IdeSessionResponse(workspaceId, "http://ide", "RUNNING");
    when(ideService.startOrReuseSession(workspaceId, user)).thenReturn(session);

    ResponseEntity<IdeSessionResponse> res = controller.startOrReuseIdeSession(workspaceId, user);

    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat(res.getBody()).isEqualTo(session);
  }
}
