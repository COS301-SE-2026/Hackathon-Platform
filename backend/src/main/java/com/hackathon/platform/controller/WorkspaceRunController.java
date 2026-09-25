package com.hackathon.platform.controller;

import com.hackathon.platform.dto.WorkspaceRunResponse;
import com.hackathon.platform.ide.WorkspaceRunService;
import com.hackathon.platform.model.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceRunController {
  private final WorkspaceRunService runService;

  @PostMapping("/{workspaceId}/run")
  @PreAuthorize("hasRole('PARTICIPANT')")
  public WorkspaceRunResponse runWorkspace(
      @PathVariable UUID workspaceId, @AuthenticationPrincipal User user) {
    return runService.runWorkspace(workspaceId, user);
  }
}
