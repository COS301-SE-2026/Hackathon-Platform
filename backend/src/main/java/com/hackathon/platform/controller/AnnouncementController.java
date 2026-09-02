package com.hackathon.platform.controller;

import com.hackathon.platform.dto.AnnouncementResponse;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.AnnouncementService;
import com.hackathon.platform.service.AnnouncementUpdateService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events/{eventId}/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
  private final AnnouncementService announcementService;
  private final AnnouncementUpdateService announcementUpdateService;

  @GetMapping
  @PreAuthorize("hasRole('PARTICIPANT')")
  public ResponseEntity<List<AnnouncementResponse>> getAnnouncements(
      @PathVariable UUID eventId, @AuthenticationPrincipal User user) {
    List<AnnouncementResponse> announcements =
        announcementService.getAnnouncements(eventId, user.getUserId());
    return ResponseEntity.ok(announcements);
  }

  @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("permitAll()")
  public SseEmitter streamAnnouncements(@PathVariable UUID eventId) {
    return announcementUpdateService.subscribe(eventId);
  }
}
