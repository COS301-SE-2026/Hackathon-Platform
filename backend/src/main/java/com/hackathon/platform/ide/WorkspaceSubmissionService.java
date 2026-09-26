package com.hackathon.platform.ide;

import com.hackathon.platform.dto.WorkspaceRunResponse;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import com.hackathon.platform.service.SubmissionCreationService;
import com.hackathon.platform.storage.ByteArrayMultipartFile;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class WorkspaceSubmissionService {
  private final CodeWorkspaceService workService;
  private final IdeContainerManager containerManager;
  private final WorkspaceInitializationService initialService;
  private final WorkspaceCodeRunner codeRunner;
  private final WorkspaceSubmissionPackager packager;
  private final SubmissionCreationService createService;

  public Map<String, String> createSubmission(UUID workspaceId, User user) {
    var workspace = workService.getWorkspaceForUser(workspaceId, user);
    IdeWorkspaceResources resources =
        IdeWorkspaceResources.forWorkspace(workspace.getWorkspaceId());
    containerManager.startOrReuse(resources);
    initialService.initializeIfNeeded(workspace.getWorkspaceId(), resources);
    WorkspaceRunResponse runRes = codeRunner.run(resources);
    WorkspaceSubmissionPackage submissionPackage = packager.createPackage(resources, runRes);
    MultipartFile outputFile =
        new ByteArrayMultipartFile(
            "outputFile", "output.json", "application/json", submissionPackage.outputJson());
    MultipartFile sourceFile =
        new ByteArrayMultipartFile(
            "sourceFile", "source.zip", "application/zip", submissionPackage.sourceZip());
    return createService.createSubmission(
        workspace.getEventId().toString(),
        workspace.getTeamId().toString(),
        outputFile,
        sourceFile,
        workspace.getLevelId(),
        user);
  }
}
