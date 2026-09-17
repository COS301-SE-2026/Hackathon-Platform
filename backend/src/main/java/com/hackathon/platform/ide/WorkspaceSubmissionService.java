package com.hackathon.platform.ide;

import com.hackathon.platform.dto.WorkspaceRunResponse;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceSubmissionService {
    private final CodeWorkspaceService workService;
    private final IdeContainerManager containerManager;
    private final WorkspaceInitializationService initialService;
    private final WorkspaceCodeRunner codeRunner;
    private final WorkspaceSubmissionPackager packager;

    public WorkspaceSubmissionPackage createSubmission(UUID workspaceId, User user) {
        var workspace = workService.getWorkspaceForUser(workspaceId, user);
        IdeWorkspaceResources resources = IdeWorkspaceResources.forWorkspace(workspace.getWorkspaceId());
        containerManager.startOrReuse(resources);
        initialService.initializeIfNeeded(workspace.getWorkspaceId(), resources);
        WorkspaceRunResponse runResult = codeRunner.run(resources);
        return packager.createPackage(resources, runResult);
    }
}