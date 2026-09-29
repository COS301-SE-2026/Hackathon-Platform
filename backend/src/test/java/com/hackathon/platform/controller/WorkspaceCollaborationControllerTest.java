package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.ide.WorkspaceCollaborationService;
import com.hackathon.platform.ide.WorkspaceEditBroadcast;
import com.hackathon.platform.ide.WorkspaceEditMessage;
import com.hackathon.platform.model.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class WorkspaceCollaborationControllerTest {
  @Mock private WorkspaceCollaborationService colabService;
  @Mock private SimpMessagingTemplate template;
  @InjectMocks private WorkspaceCollaborationController controller;

  private final UUID workspaceId = UUID.randomUUID();
  private final User user = User.builder().userId(UUID.randomUUID()).build();
  private final WorkspaceEditMessage msg = new WorkspaceEditMessage("Main.java", "code", 0);

  @Test
  void editWorkspace_appliesEditAndBroadcastsToTopic() {
    WorkspaceEditBroadcast broadcast =
        new WorkspaceEditBroadcast("Main.java", "code", 1, user.getUserId());
    when(colabService.applyEdit(workspaceId, msg, user)).thenReturn(broadcast);

    controller.editWorkspace(workspaceId, msg, new UsernamePasswordAuthenticationToken(user, null));

    verify(template).convertAndSend("/topic/workspaces/" + workspaceId, broadcast);
  }

  @Test
  void editWorkspace_throws_whenPrincipalIsNotAuthentication() {
    assertThatThrownBy(() -> controller.editWorkspace(workspaceId, msg, () -> "someone"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void editWorkspace_throws_whenPrincipalIsNotAUser() {
    var auth = new UsernamePasswordAuthenticationToken("not-a-user", null);

    assertThatThrownBy(() -> controller.editWorkspace(workspaceId, msg, auth))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getFileVersion_returnsVersion() {
    when(colabService.getVersion(workspaceId, "Main.java", user)).thenReturn(3L);

    assertThat(controller.getFileVersion(workspaceId, "Main.java", user))
        .containsEntry("version", 3L);
  }
}
