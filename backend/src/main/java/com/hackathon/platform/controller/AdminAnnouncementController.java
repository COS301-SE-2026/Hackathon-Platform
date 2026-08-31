package com.hackathon.platform.controller;

import com.hackathon.platform.dto.AnnouncementResponse;
import com.hackathon.platform.dto.CreateAnnouncementRequest;
import com.hackathon.platform.dto.CreateAnnouncementResponse;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.AnnouncementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/events/{eventId}/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {
  private final AnnouncementService announcementService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CreateAnnouncementResponse> createAnnouncement(
      @PathVariable UUID eventId,
      @AuthenticationPrincipal User admin,
      @Valid @RequestBody CreateAnnouncementRequest req) {
    CreateAnnouncementResponse res =
        announcementService.createAnnouncement(eventId, admin.getUserId(), req);
    return ResponseEntity.status(HttpStatus.CREATED).body(res);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<AnnouncementResponse>> getAnnouncements(
      @PathVariable UUID eventId, @AuthenticationPrincipal User admin) {
    List<AnnouncementResponse> announcements =
        announcementService.getAnnouncementsForAdmin(eventId, admin.getUserId());
    return ResponseEntity.ok(announcements);
  }
}
