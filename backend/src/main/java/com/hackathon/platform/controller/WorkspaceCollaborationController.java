package com.hackathon.platform.controller;

import com.hackathon.platform.ide.WorkspaceCollaborationService;
import com.hackathon.platform.ide.WorkspaceEditBroadcast;
import com.hackathon.platform.ide.WorkspaceEditMessage;
import com.hackathon.platform.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.security.Principal;
import java.util.UUID;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WorkspaceCollaborationController {
    private final WorkspaceCollaborationService colabService;
    private final SimpMessagingTemplate simpleTemplate;

    @MessageMapping("/workspaces/{workspaceId}/edit")
    public void editWorkspace(@DestinationVariable UUID workspaceId, WorkspaceEditMessage msg, Principal principal) {
        if (!(principal instanceof Authentication auth)) {
            throw new AccessDeniedException("WebSocket user not authenticated");
        }

        if (!(auth.getPrincipal() instanceof User user)) {
            throw new AccessDeniedException("Invalid WebSocket user");
        }

        WorkspaceEditBroadcast res = colabService.applyEdit(workspaceId, msg, user);
        simpleTemplate.convertAndSend("/topic/workspaces/" + workspaceId, res);
    }

    @GetMapping("/api/workspaces/{workspaceId}/collaboration/version")
    @ResponseBody
    public Map<String, Long> getFileVersion(@PathVariable UUID workspaceId, @RequestParam String path, @AuthenticationPrincipal User user) {
        long version = colabService.getVersion(workspaceId, path, user);
        return Map.of("version", version);
    }
}