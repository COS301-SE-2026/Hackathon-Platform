package com.hackathon.platform.controller;

import com.hackathon.platform.dto.WorkspaceFileContentRequest;
import com.hackathon.platform.dto.WorkspaceFileContentResponse;
import com.hackathon.platform.dto.WorkspaceFileEntry;
import com.hackathon.platform.ide.WorkspaceFileService;
import com.hackathon.platform.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceFileController {
    private final WorkspaceFileService fileService;

    @GetMapping("/{workspaceId}/files")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<List<WorkspaceFileEntry>> listFiles(@PathVariable UUID workspaceId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileService.listFiles(workspaceId, user));
    }

    @GetMapping("/{workspaceId}/files/content")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<WorkspaceFileContentResponse> readFile(@PathVariable UUID workspaceId, @RequestParam String path, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileService.readFile(workspaceId, path, user));
    }

    @PutMapping("/{workspaceId}/files/content")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<Void> writeFile(@PathVariable UUID workspaceId, @RequestBody WorkspaceFileContentRequest request, @AuthenticationPrincipal User user) {
        fileService.writeFile(workspaceId, request.path(), request.content(), user);
        return ResponseEntity.noContent().build();
    }
}
