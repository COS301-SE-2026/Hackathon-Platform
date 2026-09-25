package com.hackathon.platform.ide;

import com.hackathon.platform.dto.WorkspaceRunResponse;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceRunService {
  private final CodeWorkspaceService workService;
  private final IdeContainerManager containerManager;
  private final WorkspaceInitializationService initialService;
  private final WorkspaceCodeRunner codeRunner;

  public WorkspaceRunResponse runWorkspace(UUID workspaceId, User user) {
    var workspace = workService.getWorkspaceForUser(workspaceId, user);
    IdeWorkspaceResources resources =
        IdeWorkspaceResources.forWorkspace(workspace.getWorkspaceId());
    containerManager.startOrReuse(resources);
    initialService.initializeIfNeeded(workspace.getWorkspaceId(), resources);
    return codeRunner.run(resources);
  }
}
