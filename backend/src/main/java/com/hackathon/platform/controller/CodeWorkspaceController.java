package com.hackathon.platform.controller;

import com.hackathon.platform.dto.CodeWorkspaceResponse;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/teams/{teamId}/levels/{levelId}/workspace")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARTICIPANT')")
public class CodeWorkspaceController {
    private final CodeWorkspaceService workService;

    @PostMapping
    public ResponseEntity<CodeWorkspaceResponse> getOrCreateWorkspace(@PathVariable("eventId") UUID eventId, @PathVariable("teamId") UUID teamId, @PathVariable("levelId") short levelId, @AuthenticationPrincipal User user) {
        CodeWorkspace work = workService.getOrCreateWorkspace(eventId, teamId, levelId, user);
        return ResponseEntity.ok(CodeWorkspaceResponse.from(work));
    }
}