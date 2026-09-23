package com.hackathon.platform.controller;

import com.hackathon.platform.ide.WorkspaceSubmissionService;
import com.hackathon.platform.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceSubmissionController {
    private final WorkspaceSubmissionService subService;

    @PostMapping("/{workspaceId}/submit")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<Map<String, String>> submitWorkspace(@PathVariable UUID workspaceId, @AuthenticationPrincipal User user) {
        Map<String, String> res = subService.createSubmission(workspaceId, user);
        return ResponseEntity.ok(res);
    }
}