package com.hackathon.platform.service;

import com.hackathon.platform.dto.EventRequest;
import com.hackathon.platform.dto.EventStatusResponse;
import com.hackathon.platform.dto.ScoringPauseResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.HackathonRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class EventService {
  private final EventRepository eventRepository;
  private final HackathonRepository hackathonRepository;
  private static final Set<String>  ALLOWED_STATUSES = Set.of("UPCOMING", "ACTIVE", "COMPLETED", "CANCELED");
  private static final Set<String> TERMINAL_STATUSES = Set.of("COMPLETED", "CANCELED");
  private static final Set<String> ALLOWED_VISIBILITIES = Set.of("PUBLC", "PRIVATE");

  public EventService(EventRepository eventRepository, HackathonRepository hackathonRepository) {
    this.eventRepository = eventRepository;
    this.hackathonRepository = hackathonRepository;
  }

  private UUID getCurrentAdminId() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return user.getUserId();
  }

  /** create event */
  public Event createEvent(EventRequest req) {

    if (req == null) {
      throw new IllegalArgumentException("Event request body cannot be null.");
    }

    if (req.getName() == null || req.getName().isBlank()) {
      throw new IllegalArgumentException("Event name is required.");
    }

    if (req.getTeamSizeLimit() <= 0) {
      throw new IllegalArgumentException("Team size must be greater than 0.");
    }

    if (req.getStartDateTime() == null) {
      throw new IllegalArgumentException("Event start date is required.");
    }

    if (req.getDuration() <= 0) {
      throw new IllegalArgumentException("Event duration must be greater than 0.");
    }

    if (req.getVisibility() == null || req.getVisibility().isBlank()) {
      throw new IllegalArgumentException("Event visibility is required.");
    }

    if (req.getStatus() == null || req.getStatus().isBlank()) {
      throw new IllegalArgumentException("Event status is required.");
    }

    if(!ALLOWED_VISIBILITIES.contains(req.getVisibility())){
      throw new IllegalArgumentException("Visibility must be one of "+ALLOWED_VISIBILITIES);
    }

    if(!ALLOWED_STATUSES.contains(req.getStatus())){
      throw new IllegalArgumentException("Status must be one of "+ALLOWED_STATUSES);
    }

    if ("PRIVATE".equals(req.getVisibility())
        && (req.getRegistrationKey() == null || req.getRegistrationKey().isBlank())) {
      throw new IllegalArgumentException("Registration key is required for private events.");
    }

    if (req.getHackathonId() == null) {
      throw new IllegalArgumentException("HackathonId is required");
    }

    if (!hackathonRepository.existsById(req.getHackathonId())) {
      throw new IllegalArgumentException("Hackathon not found for this id " + req.getHackathonId());
    }

    Event event = new Event();
    event.setCreatedByUserId(getCurrentAdminId());
    event.setName(req.getName());
    event.setTeamSizeLimit(req.getTeamSizeLimit());
    event.setStartDateTime(req.getStartDateTime());
    event.setDuration(req.getDuration());
    event.setDescription(req.getDescription());
    event.setVisibility(req.getVisibility());
    event.setStatus(req.getStatus());
    event.setHackathon(req.getHackathonId());

    if ("PRIVATE".equals(req.getVisibility())) {
      event.setRegistrationKey(req.getRegistrationKey());
    } else {
      event.setRegistrationKey(null);
    }

    return eventRepository.save(event);
  }

  /** Return all events created by the current admin */
  public List<Event> getEventsByCurrentAdmin() {
    return eventRepository.fetchAllByAdmin(getCurrentAdminId());
  }

  /** return event by who created it */
  public List<Event> getEventByCreator(UUID userId) {
    return eventRepository.fetchAllByAdmin(userId);
  }

  // update Entire event based on new event information receieved in req
  public Event putUpdateEvent(UUID eventId, EventRequest req) {
    Event event =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

    event.setCreatedByUserId(getCurrentAdminId());
    event.setName(req.getName());
    event.setRegistrationKey(req.getRegistrationKey());
    event.setTeamSizeLimit(req.getTeamSizeLimit());
    event.setStartDateTime(req.getStartDateTime());
    event.setDuration(req.getDuration());
    event.setDescription(req.getDescription());
    event.setVisibility(req.getVisibility());
    event.setStatus(req.getStatus());
    if (req.getHackathonId() != null) {
      if (!hackathonRepository.existsById(req.getHackathonId())) {
        throw new RuntimeException("Hackathon not found for id " + req.getHackathonId());
      }
      event.setHackathon(req.getHackathonId());
    }

    return eventRepository.save(event);
  }

  /** Change event status/registration key/visibility */
  public Event patchEventStatus(
      UUID eventId, String visibility, String status, String registrationKey) {
    Event event =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

    if (visibility != null) {
      event.setVisibility(visibility);
    }
    if (status != null) {
      event.setStatus(status);
    }

    if (!"PUBLIC".equals(event.getVisibility())
        && registrationKey == null
        && event.getRegistrationKey() == null) {
      throw new RuntimeException("Registration key is required for private events");
    }

    if (registrationKey != null) {
      event.setRegistrationKey(registrationKey);
    }

    if ("PUBLIC".equals(event.getVisibility())) {
      event.setRegistrationKey(null);
    }

    return eventRepository.save(event);
  }

  /** Get the event status, this includes: Status and Visibility */
  public EventStatusResponse getEventStatus(UUID eventId) {
    Event event =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

    return new EventStatusResponse(event.getEventId(), event.getStatus(), event.getVisibility());
  }

  public List<Event> getOpenEventsForParticipants() {
    List<Event> ret =
        eventRepository.findByVisibilityAndStatusIn(
            "PUBLIC", List.of("UPCOMING", "ONGOING", "ACTIVE"));
    ret.addAll(
        eventRepository.findByVisibilityAndStatusIn(
            "PRIVATE", List.of("UPCOMING", "ONGOING", "ACTIVE")));
    return ret;
  }

  public List<Event> getUserActiveEvents() {
    return eventRepository.findUserActiveEvents(getCurrentAdminId());
  }

  public List<Event> getUserCompletedEvents() {
    return eventRepository.findUserCompletedEvents(getCurrentAdminId());
  }

  public List<Event> getEventsByHackathonId(UUID hackathonId) {
    if (!hackathonRepository.existsById(hackathonId)) {
      throw new RuntimeException("this Hackathon wasnnt found");
    }
    return eventRepository.findByHackathon(hackathonId);
  }

  public Event getEventById(UUID eventId) {
    return eventRepository
        .findById(eventId)
        .orElseThrow(() -> new RuntimeException("The event could not be found"));
  }

  public Event updateEventBanner(UUID eventId, String storageKey) {
    Event event =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new RuntimeException("The event could not be found"));
    event.setBannerStorageKey(storageKey);
    return eventRepository.save(event);
  }

  public Event updateEventLogo(UUID eventId, String storageKey) {
    Event event =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new RuntimeException("The event could not be found"));
    event.setLogoStorageKey(storageKey);
    return eventRepository.save(event);
  }

  public ScoringPauseResponse setScoringPaused(UUID eventId, boolean paused) {
    Event event =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event could not be found"));
    event.setScoringPaused(paused);
    eventRepository.save(event);

    return new ScoringPauseResponse(eventId, paused);
  }
}
