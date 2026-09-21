package com.hackathon.platform.controller;

import com.hackathon.platform.ide.WorkspaceCollaborationService;
import com.hackathon.platform.ide.WorkspaceEditBroadcast;
import com.hackathon.platform.ide.WorkspaceEditMessage;
import com.hackathon.platform.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WorkspaceCollaborationController {
    private final WorkspaceCollaborationService colabService;
    private final SimpMessagingTemplate simpleTemplate;

    @MessageMapping("/workspaces/{workspaceId}/edit")
    public void editWorkspace(@DestinationVariable UUID workspaceId, WorkspaceEditMessage message, @AuthenticationPrincipal User user) {
        WorkspaceEditBroadcast res = colabService.applyEdit(workspaceId, message, user);
        simpleTemplate.convertAndSend("/topic/workspaces/" + workspaceId, res);
    }
}