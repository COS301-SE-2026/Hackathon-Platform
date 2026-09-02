package com.hackathon.platform.controller;

import com.hackathon.platform.dto.EventParticipantResponse;
import com.hackathon.platform.dto.EventRequest;
import com.hackathon.platform.dto.EventStatusResponse;
import com.hackathon.platform.dto.ExtendTimerRequest;
import com.hackathon.platform.dto.ExtendTimerResponse;
import com.hackathon.platform.dto.ScoringPauseResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.EventService;
import com.hackathon.platform.service.TeamService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {
  private final EventService eventService;
  private final TeamService teamService;

  public AdminEventController(EventService eventService, TeamService teamService) {
    this.eventService = eventService;
    this.teamService = teamService;
  }

  /** Create an event /api/admin/events */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Event> createEvent(@RequestBody EventRequest req) {
    Event newEvent = eventService.createEvent(req);
    return ResponseEntity.ok(newEvent);
  }

  /** Get all events created by admin /api/admin/events */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Event>> getEvents(@AuthenticationPrincipal User user) {
    List<Event> events = eventService.getEventByCreator(user.getUserId());
    return ResponseEntity.ok(events);
  }

  /** Update existing event /api/admin/events/{id} */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Event> putUpdateEvent(
      @PathVariable("id") UUID eventId, @RequestBody EventRequest req) {
    Event updatedEvent = eventService.putUpdateEvent(eventId, req);
    return ResponseEntity.ok(updatedEvent);
  }

  /**
   * Update existing event status and visibility (also registration if need)
   * /api/admin/events/{id}/status
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Event> patchEventStatus(
      @PathVariable("id") UUID eventId, @RequestBody EventRequest req) {
    return ResponseEntity.ok(
        eventService.patchEventStatus(
            eventId, req.getVisibility(), req.getStatus(), req.getRegistrationKey()));
  }

  /** Retrieve event status and event visbility /api/admin/events/{id}/status */
  @GetMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<EventStatusResponse> getEventStatus(@PathVariable("id") UUID eventId) {
    return ResponseEntity.ok(eventService.getEventStatus(eventId));
  }

  @PatchMapping("/{id}/scoring/pause")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ScoringPauseResponse> pauseScoring(@PathVariable("id") UUID eventId) {
    return ResponseEntity.ok(eventService.setScoringPaused(eventId, true));
  }

  @PatchMapping("/{id}/scoring/resume")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ScoringPauseResponse> resumeScoring(@PathVariable("id") UUID eventId) {
    return ResponseEntity.ok(eventService.setScoringPaused(eventId, false));
  }

  @GetMapping("/{id}/participants")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<EventParticipantResponse>> getEventParticipants(
      @PathVariable("id") UUID eventId) {
    return ResponseEntity.ok(teamService.listEventParticipants(eventId));
  }

  @PatchMapping("/{id}/extend")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ExtendTimerResponse> extendTimer(
      @PathVariable("id") UUID eventId, @RequestBody ExtendTimerRequest req) {
    Event event = eventService.extendEvent(eventId, req.getAdditionalTime());
    return ResponseEntity.ok(
        new ExtendTimerResponse(event.getEventId(), event.getDuration(), event.getEndDateTime()));
  }
}
