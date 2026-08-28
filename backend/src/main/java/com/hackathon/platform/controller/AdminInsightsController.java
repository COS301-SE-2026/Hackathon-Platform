package com.hackathon.platform.controller;

import com.hackathon.platform.dto.AdminDashboardResponse;
import com.hackathon.platform.dto.EventInsightsResponse;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.InsightsService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminInsightsController {

  private final InsightsService insightsService;

  public AdminInsightsController(InsightsService insightsService) {
    this.insightsService = insightsService;
  }

  /** wide overview across every event by an admin */
  @GetMapping("/api/admin/dashboard")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AdminDashboardResponse> getAdminDashboard(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(insightsService.getAdminDashboard(user.getUserId()));
  }

  /** Per event dashboard stats for admin */
  @GetMapping("/api/admin/events/{id}/insights")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<EventInsightsResponse> getEventInsights(
      @PathVariable("id") UUID eventId,
      @RequestParam(name = "trendWindowMinutes", defaultValue = "60") int trendWindowMinutes) {
    return ResponseEntity.ok(insightsService.getEventInsights(eventId, trendWindowMinutes));
  }
}
