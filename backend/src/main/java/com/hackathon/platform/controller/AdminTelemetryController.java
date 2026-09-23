package com.hackathon.platform.controller;

import com.hackathon.platform.service.TelemetryRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/workspaces")
@RequiredArgsConstructor
public class AdminTelemetryController {
    private final TelemetryRiskService riskService;

    @GetMapping("/{workspaceId}/participants/{userId}/telemetry/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public TelemetryRiskService.TelemetryRiskReport getReport(@PathVariable UUID workspaceId, @PathVariable UUID userId) {
        return riskService.analyze(workspaceId, userId);
    }
}