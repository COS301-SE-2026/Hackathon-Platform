package com.hackathon.platform.controller;

import com.hackathon.platform.dto.IdeSessionResponse;
import com.hackathon.platform.ide.IdeSessionService;
import com.hackathon.platform.model.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class IdeSessionController {
  private final IdeSessionService ideService;

  @PostMapping("/{workspaceId}/ide-session")
  @PreAuthorize("hasRole('PARTICIPANT')")
  public ResponseEntity<IdeSessionResponse> startOrReuseIdeSession(
      @PathVariable UUID workspaceId, @AuthenticationPrincipal User user) {
    IdeSessionResponse res = ideService.startOrReuseSession(workspaceId, user);
    return ResponseEntity.ok(res);
  }
}
