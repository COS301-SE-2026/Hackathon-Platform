package com.hackathon.platform.service;

import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.CodeWorkspaceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CodeWorkspaceService {
    private final CodeWorkspaceRepository workspaceRepo;
    private final CodeWorkspaceAccessService accService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CodeWorkspace getOrCreateWorkspace(UUID eventId, UUID teamId, short levelId, User user) {
        if (user == null || user.getUserId() == null) {
            throw new AccessDeniedException("you are not authenticated");
        }

        if (eventId == null || teamId == null) {
            throw new IllegalArgumentException("both event Id and team Id are required");
        }

        UUID userId = user.getUserId();
        UUID hackId = accService.requireParticipantAccess(eventId, teamId, levelId, userId);

        workspaceRepo.createIfMissing(eventId, teamId, hackId, levelId, userId);

        return workspaceRepo.findByEventIdAndTeamIdAndLevelId(eventId, teamId, levelId).orElseThrow(() -> new IllegalStateException("Workspace could not load"));
    }

    @Transactional(readOnly = true)
    public CodeWorkspace getWorkspaceForUser(UUID workspaceId, User user) {
        if (user == null || user.getUserId() == null) {
            throw new AccessDeniedException("you are not authenticated");
        }

        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID is required");
        }

        CodeWorkspace work = workspaceRepo.findById(workspaceId).orElseThrow(() -> new IllegalArgumentException("Could not find the workspace"));

        UUID hackId = accService.requireParticipantAccess(work.getEventId(), work.getTeamId(), work.getLevelId(), user.getUserId());

        if (!hackId.equals(work.getHackathonId())) {
            throw new AccessDeniedException("Workspace does not match the event's hackathon");
        }

        return work;
    }
}