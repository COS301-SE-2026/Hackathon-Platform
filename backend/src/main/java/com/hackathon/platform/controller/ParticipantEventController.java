package com.hackathon.platform.controller;

import com.hackathon.platform.model.Event;
import com.hackathon.platform.service.EventService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import com.hackathon.platform.dto.EventRegistrationResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.hackathon.platform.model.User;
import com.hackathon.platform.dto.EventRegistrationRequest;
import org.springframework.http.HttpStatus;
import com.hackathon.platform.service.EventRegistrationService;

/** Participant-facing event endpoints. */
@RestController
@RequestMapping("/api/events")
public class ParticipantEventController {

  private final EventService eventService;
  private final EventRegistrationService eventRegistrationService;

  public ParticipantEventController(EventService eventService, EventRegistrationService eventRegistrationService) {
    this.eventService = eventService;
    this.eventRegistrationService = eventRegistrationService;
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

  @PostMapping("/{eventId}/registered")
  public ResponseEntity<EventRegistrationResponse> registerForEvent(@PathVariable UUID eventId, @RequestBody(required = false) EventRegistrationRequest req,@AuthenticationPrincipal User currUser){
    String regKey = req == null ? null : req.getRegKey();
    EventRegistrationResponse resp = eventRegistrationService.registerForEvent(eventId, currUser.getUserId(), regKey);
    return ResponseEntity.status(HttpStatus.CREATED).body(resp);
  }

  @GetMapping("/my-registrations")
  public ResponseEntity<List<EventRegistrationResponse>> getMyRegistrations(@AuthenticationPrincipal User currUser){
    return ResponseEntity.ok(eventRegistrationService.getMyRegistrations(currUser.getUserId()));
  }
}
