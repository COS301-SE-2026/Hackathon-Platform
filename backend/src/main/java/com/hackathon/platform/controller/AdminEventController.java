package com.hackathon.platform.controller;

import com.hackathon.platform.dto.EventRequest;
import com.hackathon.platform.dto.EventStatusResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.service.EventService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

  public AdminEventController(EventService eventService) {
    this.eventService = eventService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Event> createEvent(@RequestBody EventRequest req) {
    Event newEvent = eventService.createEvent(req);
    return ResponseEntity.ok(newEvent);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Event>> getEvents(@PathVariable("id") UUID createdByUserId) {
    List<Event> events = eventService.getEventByCreator(createdByUserId);
    return ResponseEntity.ok(events);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Event> putUpdateEvent(
      @PathVariable("id") UUID eventId, @RequestBody EventRequest req) {
    Event updatedEvent = eventService.putUpdateEvent(eventId, req);
    return ResponseEntity.ok(updatedEvent);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Event> patchEventStatus(
      @PathVariable("id") UUID eventId, @RequestBody EventRequest req) {
    return ResponseEntity.ok(
        eventService.patchEventStatus(
            eventId, req.getVisibility(), req.getStatus(), req.getRegistrationKey()));
  }

  @GetMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<EventStatusResponse> getEventStatus(@PathVariable("id") UUID eventId) {
    return ResponseEntity.ok(eventService.getEventStatus(eventId));
  }
}
