package com.hackathon.platform.controller;

import com.hackathon.platform.ide.TelemetryBatchRequest;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.WorkspaceTelemetryService;
import com.hackathon.platform.service.TelemetryFeatureService;
import com.hackathon.platform.service.TelemetryRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/telemetry")
@RequiredArgsConstructor
public class WorkspaceTelemetryController {
    private final WorkspaceTelemetryService telService;
    private final TelemetryFeatureService featureService;
    private final TelemetryRiskService riskService;

    @PostMapping("/sessions")
    public Map<String, UUID> startSession(@PathVariable UUID workspaceId, @AuthenticationPrincipal User user) {
        UUID sessionId = telService.startSession(workspaceId, user);
        return Map.of("sessionId", sessionId);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveBatch(@PathVariable UUID workspaceId, @RequestBody TelemetryBatchRequest req, @AuthenticationPrincipal User user) {
        telService.saveBatch(workspaceId, req, user);
    }

    @PostMapping("/sessions/{sessionId}/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endSession(@PathVariable UUID workspaceId, @PathVariable UUID sessionId, @AuthenticationPrincipal User user) {
        telService.endSession(workspaceId, sessionId, user);
    }

    @GetMapping("/features")
    public TelemetryFeatureService.TelemetryFeatures getFeatures(@PathVariable UUID workspaceId, @AuthenticationPrincipal User user) {
        return featureService.extractFeatures(workspaceId, user.getUserId());
    }
}