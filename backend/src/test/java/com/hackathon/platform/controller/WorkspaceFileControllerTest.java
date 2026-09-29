package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.WorkspaceFileContentRequest;
import com.hackathon.platform.dto.WorkspaceFileContentResponse;
import com.hackathon.platform.dto.WorkspaceFileEntry;
import com.hackathon.platform.ide.WorkspaceFileService;
import com.hackathon.platform.model.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceFileControllerTest {
  @Mock private WorkspaceFileService fileService;
  @InjectMocks private WorkspaceFileController controller;

  private final UUID workspaceId = UUID.randomUUID();
  private final User user = User.builder().userId(UUID.randomUUID()).build();

  @Test
  void listFiles_returnsFiles() {
    List<WorkspaceFileEntry> files =
        List.of(new WorkspaceFileEntry("Main.java", "Main.java", false));
    when(fileService.listFiles(workspaceId, user)).thenReturn(files);

    assertThat(controller.listFiles(workspaceId, user).getBody()).isEqualTo(files);
  }

  @Test
  void readFile_returnsContent() {
    WorkspaceFileContentResponse content = new WorkspaceFileContentResponse("Main.java", "code");
    when(fileService.readFile(workspaceId, "Main.java", user)).thenReturn(content);

    assertThat(controller.readFile(workspaceId, "Main.java", user).getBody()).isEqualTo(content);
  }

  @Test
  void writeFile_savesAndReturnsNoContent() {
    WorkspaceFileContentRequest req = new WorkspaceFileContentRequest("Main.java", "code");

    var res = controller.writeFile(workspaceId, req, user);

    assertThat(res.getStatusCode().value()).isEqualTo(204);
    verify(fileService).writeFile(workspaceId, "Main.java", "code", user);
  }
}
