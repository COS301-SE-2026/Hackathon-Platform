package com.hackathon.platform.controller;

import com.hackathon.platform.dto.EventRegistrationRequest;
import com.hackathon.platform.dto.EventRegistrationResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CertificateService;
import com.hackathon.platform.service.EventRegistrationService;
import com.hackathon.platform.service.EventService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Participant-facing event endpoints. */
@RestController
@RequestMapping("/api/events")
public class ParticipantEventController {

  private final EventService eventService;
  private final EventRegistrationService eventRegistrationService;
  private final CertificateService certServ;

  public ParticipantEventController(
      EventService eventService,
      EventRegistrationService eventRegistrationService,
      CertificateService certServ) {
    this.eventService = eventService;
    this.eventRegistrationService = eventRegistrationService;
    this.certServ = certServ;
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
  public ResponseEntity<EventRegistrationResponse> registerForEvent(
      @PathVariable UUID eventId,
      @RequestBody(required = false) EventRegistrationRequest req,
      @AuthenticationPrincipal User currUser) {
    String regKey = req == null ? null : req.getRegKey();
    EventRegistrationResponse resp =
        eventRegistrationService.registerForEvent(
            eventId,
            currUser.getUserId(),
            regKey,
            req == null ? null : req.getDietaryReq(),
            req == null ? null : req.getAllergies());
    return ResponseEntity.status(HttpStatus.CREATED).body(resp);
  }

  @GetMapping("/my-registrations")
  public ResponseEntity<List<EventRegistrationResponse>> getMyRegistrations(
      @AuthenticationPrincipal User currUser) {
    return ResponseEntity.ok(eventRegistrationService.getMyRegistrations(currUser.getUserId()));
  }

  @GetMapping("/{eventId}/certificate")
  public ResponseEntity<byte[]> getParticipationCertificate(
      @PathVariable UUID eventId, @AuthenticationPrincipal User user) {
    byte[] pdf = certServ.genCertificate(eventId, user);
    String file =
        "certificate-" + user.getFirstName().toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".pdf";
    ContentDisposition disposition = ContentDisposition.inline().filename(file).build();

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(pdf);
  }
}
