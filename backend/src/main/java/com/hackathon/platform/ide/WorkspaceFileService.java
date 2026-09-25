package com.hackathon.platform.ide;

import com.hackathon.platform.dto.WorkspaceFileContentResponse;
import com.hackathon.platform.dto.WorkspaceFileEntry;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceFileService {
  private final CodeWorkspaceService workService;
  private final IdeContainerManager containerManager;
  private final WorkspaceFileStore fileStore;

  public List<WorkspaceFileEntry> listFiles(UUID workspaceId, User user) {
    IdeWorkspaceResources resources = getResources(workspaceId, user);
    return fileStore.listFiles(resources);
  }

  public WorkspaceFileContentResponse readFile(UUID workspaceId, String path, User user) {
    IdeWorkspaceResources resources = getResources(workspaceId, user);
    String content = fileStore.readFile(resources, path);
    return new WorkspaceFileContentResponse(path, content);
  }

  public void writeFile(UUID workspaceId, String path, String content, User user) {
    IdeWorkspaceResources resources = getResources(workspaceId, user);
    fileStore.writeFile(resources, path, content);
  }

  private IdeWorkspaceResources getResources(UUID workspaceId, User user) {
    CodeWorkspace workspace = workService.getWorkspaceForUser(workspaceId, user);
    IdeWorkspaceResources resources =
        IdeWorkspaceResources.forWorkspace(workspace.getWorkspaceId());
    containerManager.startOrReuse(resources);
    return resources;
  }
}
