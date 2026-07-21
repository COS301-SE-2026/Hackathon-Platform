package com.hackathon.platform.controller;

import com.hackathon.platform.model.Event;
import com.hackathon.platform.service.EventService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

/** Participant-facing event endpoints. */
@RestController
@RequestMapping("/api/events")
public class ParticipantEventController {

  private final EventService eventService;

  public ParticipantEventController(EventService eventService) {
    this.eventService = eventService;
  }

  /** Get open events visible to participants. */
  @GetMapping("/open")
  public ResponseEntity<List<Event>> getOpenEvents() {
    return ResponseEntity.ok(eventService.getOpenEventsForParticipants());
  }

  @GetMapping("/user-active-events")
  public ResponseEntity<List<Event>> getUserActiveEvents() {
    return ResponseEntity.ok(eventService.getUserActiveEvents());
  }

  @GetMapping("/completed")
  public ResponseEntity<List<Event>> getUserCompletedEvents() {
    return ResponseEntity.ok(eventService.getUserCompletedEvents());
  }

  @GetMapping("/{eventId}")
  public ResponseEntity<Event> getEventById(@PathVariable UUID eventId) {
    return ResponseEntity.ok(eventService.getEventById(eventId));
  }
}
