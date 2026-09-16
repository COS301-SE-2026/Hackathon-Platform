package com.hackathon.platform.ide;

import com.hackathon.platform.dto.IdeSessionResponse;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdeSessionService {
    private final CodeWorkspaceService workService;
    private final IdeContainerManager management;
    private final IdeAccessManager accManager;

    public IdeSessionResponse startOrReuseSession(UUID workspaceId, User user) {
        CodeWorkspace work = workService.getWorkspaceForUser(workspaceId, user);
        IdeWorkspaceResources resources = IdeWorkspaceResources.forWorkspace(work.getWorkspaceId());
        IdeContainerSession session = management.startOrReuse(resources);
        String ideUrl = accManager.getIdeUrl(session);

        return new IdeSessionResponse(session.workspaceId(), ideUrl, session.status());
    }
}