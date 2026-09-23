package com.hackathon.platform.controller;

import com.hackathon.platform.service.TelemetryRiskService;
import com.hackathon.platform.repository.CodeWorkspaceRepository;
import com.hackathon.platform.repository.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminTelemetryController {
    private final TelemetryRiskService riskService;
    private final CodeWorkspaceRepository workspaceRepo;
    private final LevelRepository levelRepo;

    @GetMapping("/workspaces/{workspaceId}/participants/{userId}/telemetry/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public TelemetryRiskService.TelemetryRiskReport getReport(@PathVariable UUID workspaceId, @PathVariable UUID userId) {
        return riskService.analyze(workspaceId, userId);
    }

    @GetMapping("/events/{eventId}/teams/{teamId}/workspaces")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public List<AdminWorkspaceResponse> getTeamWorkspaces(@PathVariable UUID eventId, @PathVariable UUID teamId) {
        return workspaceRepo.findByEventIdAndTeamIdOrderByLevelIdAsc(eventId, teamId).stream().map(workspace -> {
            var level = levelRepo.findById(workspace.getLevelId()).orElseThrow(() -> new IllegalStateException("Level could not be found"));
            return new AdminWorkspaceResponse(workspace.getWorkspaceId(), workspace.getLevelId(), level.getLevelNumber());
        }).toList();
    }

    public record AdminWorkspaceResponse(UUID workspaceId, short levelId, short levelNumber) {}
}