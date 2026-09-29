package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.WorkspaceFileContentResponse;
import com.hackathon.platform.dto.WorkspaceFileEntry;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class WorkspaceFileServiceTest {
  @Mock private CodeWorkspaceService workService;
  @Mock private IdeContainerManager containerManager;
  @Mock private WorkspaceFileStore fileStore;
  private WorkspaceFileService service;

  private final UUID workspaceId = UUID.randomUUID();
  private final User user = User.builder().userId(UUID.randomUUID()).build();
  private final IdeWorkspaceResources resources = IdeWorkspaceResources.forWorkspace(workspaceId);

  @BeforeEach
  void setUp() {
    service = new WorkspaceFileService(workService, containerManager, fileStore);
  }

  private void allowAccess() {
    CodeWorkspace workspace = mock(CodeWorkspace.class);
    when(workspace.getWorkspaceId()).thenReturn(workspaceId);
    when(workService.getWorkspaceForUser(workspaceId, user)).thenReturn(workspace);
  }

  @Test
  void listFiles_returnsFilesFromStore() {
    allowAccess();
    List<WorkspaceFileEntry> files =
        List.of(new WorkspaceFileEntry("Main.java", "Main.java", false));
    when(fileStore.listFiles(resources)).thenReturn(files);

    assertThat(service.listFiles(workspaceId, user)).isEqualTo(files);
    verify(containerManager).startOrReuse(resources);
  }

  @Test
  void readFile_returnsPathAndContent() {
    allowAccess();
    when(fileStore.readFile(resources, "Main.java")).thenReturn("class Main {}");

    WorkspaceFileContentResponse res = service.readFile(workspaceId, "Main.java", user);

    assertThat(res.path()).isEqualTo("Main.java");
    assertThat(res.content()).isEqualTo("class Main {}");
    verify(containerManager).startOrReuse(resources);
  }

  @Test
  void writeFile_writesToStore() {
    allowAccess();

    service.writeFile(workspaceId, "Main.java", "class Main {}", user);

    verify(containerManager).startOrReuse(resources);
    verify(fileStore).writeFile(resources, "Main.java", "class Main {}");
  }

  @Test
  void listFiles_throws_whenUserHasNoAccess() {
    when(workService.getWorkspaceForUser(workspaceId, user))
        .thenThrow(new AccessDeniedException("nope"));

    assertThatThrownBy(() -> service.listFiles(workspaceId, user))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(containerManager, fileStore);
  }
}
