package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.WorkspaceRunResponse;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import com.hackathon.platform.service.SubmissionCreationService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class WorkspaceSubmissionServiceTest {
  @Mock private CodeWorkspaceService workService;
  @Mock private IdeContainerManager containerManager;
  @Mock private WorkspaceInitializationService initService;
  @Mock private WorkspaceCodeRunner codeRunner;
  @Mock private WorkspaceSubmissionPackager packager;
  @Mock private SubmissionCreationService createService;
  private WorkspaceSubmissionService service;

  private final UUID workspaceId = UUID.randomUUID();
  private final UUID eventId = UUID.randomUUID();
  private final UUID teamId = UUID.randomUUID();
  private final User user = User.builder().userId(UUID.randomUUID()).build();
  private final IdeWorkspaceResources resources = IdeWorkspaceResources.forWorkspace(workspaceId);
  private final WorkspaceRunResponse runResponse = new WorkspaceRunResponse(true, 0, "{}", "");

  @BeforeEach
  void setUp() {
    service =
        new WorkspaceSubmissionService(
            workService, containerManager, initService, codeRunner, packager, createService);

    CodeWorkspace workspace = mock(CodeWorkspace.class);
    when(workspace.getWorkspaceId()).thenReturn(workspaceId);
    when(workService.getWorkspaceForUser(workspaceId, user)).thenReturn(workspace);
    when(codeRunner.run(resources)).thenReturn(runResponse);
    // only used by the happy path, so lenient
    lenient().when(workspace.getEventId()).thenReturn(eventId);
    lenient().when(workspace.getTeamId()).thenReturn(teamId);
    lenient().when(workspace.getLevelId()).thenReturn((short) 3);
  }

  @Test
  void createSubmission_packagesRunResultAndCreatesSubmission() {
    WorkspaceSubmissionPackage pkg =
        new WorkspaceSubmissionPackage(new byte[] {1}, "{}".getBytes());
    when(packager.createPackage(resources, runResponse)).thenReturn(pkg);
    when(createService.createSubmission(
            eq(eventId.toString()),
            eq(teamId.toString()),
            any(MultipartFile.class),
            any(MultipartFile.class),
            eq((short) 3),
            eq(user)))
        .thenReturn(Map.of("submissionId", "abc"));

    Map<String, String> res = service.createSubmission(workspaceId, user);

    assertThat(res).containsEntry("submissionId", "abc");
  }

  @Test
  void createSubmission_throws_whenPackagingFails() {
    when(packager.createPackage(resources, runResponse))
        .thenThrow(new IllegalStateException("Execution failed"));

    assertThatThrownBy(() -> service.createSubmission(workspaceId, user))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(createService);
  }
}
