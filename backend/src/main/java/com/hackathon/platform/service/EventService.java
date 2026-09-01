package com.hackathon.platform.service;

import com.hackathon.platform.dto.EventRequest;
import com.hackathon.platform.dto.EventStatusResponse;
import com.hackathon.platform.dto.ScoringPauseResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.HackathonRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.repository.TeamRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
  private final EventRepository eventRepository;
  private final HackathonRepository hackathonRepository;
  private final EventRegistrationRepository eventRegRepo;
  private final TeamRepository teamRepo;
  private final SubmissionRepository subRepo;
  private static final Set<String> ALLOWED_STATUSES =
      Set.of("UPCOMING", "ACTIVE", "COMPLETED", "CANCELED");
  private static final Set<String> TERMINAL_STATUSES = Set.of("COMPLETED", "CANCELED");
  private static final Set<String> ALLOWED_VISIBILITIES = Set.of("PUBLIC", "PRIVATE");

  @Autowired
  public EventService(
      EventRepository eventRepository,
      HackathonRepository hackathonRepository,
      EventRegistrationRepository eventRegRepo,
      TeamRepository teamRepo,
      SubmissionRepository subRepo) {
    this.eventRepository = eventRepository;
    this.hackathonRepository = hackathonRepository;
    this.eventRegRepo = eventRegRepo;
    this.teamRepo = teamRepo;
    this.subRepo = subRepo;
  }

  public EventService(EventRepository eventRepository, HackathonRepository hackathonRepository) {
    this(eventRepository, hackathonRepository, null, null, null);
  }

  private UUID getCurrentAdminId() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return user.getUserId();
  }

  /** create event */
  @Transactional
  public Event createEvent(EventRequest req) {
    validateEventReq(req, true);
    requireHackathon(req.getHackathonId());
    Event event = new Event();
    event.setHackathon(req.getHackathonId());
    event.setCreatedByUserId(getCurrentAdminId());
    applyReq(event, req, true);
    event.setStatus(
        calculateLifecycleStatus(event, OffsetDateTime.now(ZoneOffset.UTC), req.getStatus()));
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
  @Transactional
  public Event putUpdateEvent(UUID eventId, EventRequest req) {
    Event event = getEventById(eventId);
    validateEventReq(req, false);
    if (req.getHackathonId() != null) {
      requireHackathon(req.getHackathonId());
      event.setHackathon(req.getHackathonId());
    }

    if (req.getStatus() != null) {
      assertValidStatusTransition(event.getStatus(), req.getStatus());
    }

    applyReq(event, req, false);
    return eventRepository.save(event);
  }

  /** Change event status/registration key/visibility */
  public Event patchEventStatus(
      UUID eventId, String visibility, String status, String registrationKey) {
    Event event = getEventById(eventId);

    if (visibility != null) {
      if (!ALLOWED_VISIBILITIES.contains(visibility)) {
        throw new IllegalArgumentException("Invalid visibility: " + visibility);
      }
      event.setVisibility(visibility);
    }

    if (status != null) {
      assertValidStatusTransition(event.getStatus(), status);
      event.setStatus(status);
    }

    if ("PRIVATE".equals(event.getVisibility())) {
      if (registrationKey != null && !registrationKey.isBlank()) {
        event.setRegistrationKey(registrationKey.trim());
      } else if (event.getRegistrationKey() != null && !event.getRegistrationKey().isBlank()) {
        throw new IllegalArgumentException("Registration key is required");
      }
    } else {
      event.setRegistrationKey(null);
    }
    return eventRepository.save(event);
  }

  /** Get the event status, this includes: Status and Visibility */
  public EventStatusResponse getEventStatus(UUID eventId) {
    Event event = getEventById(eventId);
    refreshLifecycleStatus(event, OffsetDateTime.now(ZoneOffset.UTC));
    return new EventStatusResponse(event.getEventId(), event.getStatus(), event.getVisibility());
  }

  public List<Event> getOpenEventsForParticipants() {
    return eventRepository.findByVisibilityAndStatusIn("PUBLIC", List.of("UPCOMING", "ACTIVE"));
  }

  public List<Event> getPrivateEvents() {
    return eventRepository.findByVisibilityAndStatusIn("PRIVATE", List.of("UPCOMING", "ACTIVE"));
  }

  public List<Event> getUserActiveEvents() {
    return eventRepository.findUserActiveEvents(getCurrentAdminId());
  }

  public List<Event> getUserCompletedEvents() {
    return eventRepository.findUserCompletedEvents(getCurrentAdminId());
  }

  public List<Event> getEventsByHackathonId(UUID hackathonId) {
    requireHackathon(hackathonId);
    return eventRepository.findByHackathon(hackathonId);
  }

  @Transactional
  public Event getEventById(UUID eventId) {
    Event event =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    refreshLifecycleStatus(event, OffsetDateTime.now(ZoneOffset.UTC));
    return event;
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

  @Transactional
  public Event extendEvent(UUID eventId, int sec){
    if(sec<=0){
      throw new IllegalArgumentException("Invalid time");
    }
    Event event = getEventById(eventId);
    if("CANCELED".equals(event.getStatus())){
      throw new IllegalArgumentException("Event was canceled");
    }
    event.setDuration(event.getDuration()+sec);
    refreshLifecycleStatus(event, OffsetDateTime.now(ZoneOffset.UTC));
    return eventRepository.save(event);
  }

  private void assertValidStatusTransition(String currStat, String newStat) {
    if (!ALLOWED_STATUSES.contains(newStat)) {
      throw new IllegalArgumentException("New status must be one of " + ALLOWED_STATUSES);
    }
    if (currStat != null && TERMINAL_STATUSES.contains(currStat) && !newStat.equals(currStat)) {
      throw new IllegalArgumentException("Event is " + currStat + " it cant change to " + newStat);
    }
  }

  private String calculateLifecycleStatus(Event event, OffsetDateTime now, String reqStatus) {
    if ("CANCELED".equals(reqStatus)) {
      return "CANCELED";
    }
    OffsetDateTime start = event.getStartDateTime();
    OffsetDateTime end = start.plusSeconds(event.getDuration());
    if (!now.isBefore(end)) {
      return "COMPLETED";
    }
    if (!now.isBefore(start)) {
      return "ACTIVE";
    }
    return "UPCOMING";
  }

  @Transactional
  public boolean refreshLifecycleStatus(Event event, OffsetDateTime now) {
    if ("CANCELED".equals(event.getStatus())) {
      return false;
    }
    String next = calculateLifecycleStatus(event, now, event.getStatus());
    if (!next.equals(event.getStatus())) {
      event.setStatus(next);
      eventRepository.save(event);
      return true;
    }
    return false;
  }

  private void validateEventReq(EventRequest req, boolean creating) {
    if (req == null) {
      throw new IllegalArgumentException("Event request body cant be null");
    }
    if (creating && req.getHackathonId() == null) {
      throw new IllegalArgumentException("HackathonId is required");
    }
    if (creating || req.getName() != null) {
      if (req.getName() == null || req.getName().isBlank()) {
        throw new IllegalArgumentException("Name is required");
      }
    }
    if (creating || req.getTeamSizeLimit() > 0) {
      if (req.getTeamSizeLimit() <= 0) {
        throw new IllegalArgumentException("Team size limit must be greater than 0");
      }
    }
    if (creating || req.getStartDateTime() != null) {
      if (req.getStartDateTime() == null) {
        throw new IllegalArgumentException("Start date is required");
      }
    }
    if (creating || req.getDuration() > 0) {
      if (req.getDuration() <= 0) {
        throw new IllegalArgumentException("Duration is less than 0");
      }
    }
    if (req.getVisibility() != null && !ALLOWED_VISIBILITIES.contains(req.getVisibility())) {
      throw new IllegalArgumentException(
          "Visibility must be one of these: " + ALLOWED_VISIBILITIES);
    }
    if (req.getStatus() != null && !ALLOWED_STATUSES.contains(req.getStatus())) {
      throw new IllegalArgumentException("Status must be one of these: " + ALLOWED_STATUSES);
    }
    if (creating && req.getVisibility() == null) {
      throw new IllegalArgumentException("Visibility is required");
    }
    if ("PRIVATE".equals(req.getVisibility())
        && (req.getRegistrationKey() == null || req.getRegistrationKey().isBlank())) {
      throw new IllegalArgumentException("Registration key is required for private events");
    }
    validatePrize(req.getFirstPlacePrize(), "First place prize");
    validatePrize(req.getSecondPlacePrize(), "Second place prize");
    validatePrize(req.getThirdPlacePrize(), "Third place prize");
    validatePrize(req.getTotalPrizePool(), "Total place prize");
  }

  private void validatePrize(BigDecimal value, String name) {
    if (value != null && value.signum() < 0) {
      throw new IllegalArgumentException("Prize must be greater than 0");
    }
  }

  private void applyReq(Event event, EventRequest req, boolean creating) {
    if (req.getName() != null) {
      event.setName(req.getName());
    }
    if (req.getTeamSizeLimit() > 0) {
      event.setTeamSizeLimit(req.getTeamSizeLimit());
    }
    if (req.getStartDateTime() != null) {
      event.setStartDateTime(req.getStartDateTime());
    }
    if (req.getDuration() > 0) {
      event.setDuration(req.getDuration());
    }
    if (req.getVisibility() != null) {
      event.setVisibility(req.getVisibility());
    }
    if (req.getRegistrationKey() != null || "PUBLIC".equals(req.getVisibility())) {
      event.setRegistrationKey(
          "PRIVATE".equals(event.getVisibility()) ? req.getRegistrationKey() : null);
    }
    if (req.getStatus() != null && !creating) {
      event.setStatus(req.getStatus());
    }
    if (req.getAllowedTech() != null) {
      event.setAllowedTech(req.getAllowedTech());
    }
    if (req.getRules() != null) {
      event.setRules(req.getRules());
    }
    if (req.getTagline() != null) {
      event.setTagline(req.getTagline());
    }
    if (req.getFirstPlacePrize() != null) {
      event.setFirstPlacePrize(req.getFirstPlacePrize());
    }
    if (req.getSecondPlacePrize() != null) {
      event.setSecondPlacePrize(req.getSecondPlacePrize());
    }
    if (req.getThirdPlacePrize() != null) {
      event.setThirdPlacePrize(req.getThirdPlacePrize());
    }
    if (req.getTotalPrizePool() != null) {
      event.setTotalPrizePool(req.getTotalPrizePool());
    }
    if (req.getFreezeTime() != null) {
      event.setLeaderboardFreezeDateTime(req.getFreezeTime());
    }
    if (req.getInPerson() != null) {
      event.setInPerson(req.getInPerson());
    }
    if (event.getAllowedTech() == null) {
      event.setAllowedTech(new ArrayList<>());
    }
  }

  private List<String> normalizeTech(List<String> tech) {
    return tech.stream()
        .filter(java.util.Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .distinct()
        .limit(100)
        .collect(Collectors.toList());
  }

  private void requireHackathon(UUID id) {
    if (id == null || !hackathonRepository.existsById(id)) {
      throw new IllegalArgumentException("Hackathon not found");
    }
  }
}
