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
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class EventService {
  private final EventRepository eventRepository;
  private final HackathonRepository hackathonRepository;
  private final EventRegistrationRepository eventRegRepo;
  private final TeamRepository teamRepo;
  private final SubmissionRepository subRepo;
  private static final Set<String>  ALLOWED_STATUSES = Set.of("UPCOMING", "ACTIVE", "COMPLETED", "CANCELED");
  private static final Set<String> TERMINAL_STATUSES = Set.of("COMPLETED", "CANCELED");
  private static final Set<String> ALLOWED_VISIBILITIES = Set.of("PUBLC", "PRIVATE");

  @Autowired
  public EventService(EventRepository eventRepository, HackathonRepository hackathonRepository, EventRegistrationRepository eventRegRepo, TeamRepository teamRepo, SubmissionRepository subRepo) {
    this.eventRepository = eventRepository;
    this.hackathonRepository = hackathonRepository;
    this.eventRegRepo = eventRegRepo;
    this.teamRepo = teamRepo;
    this.subRepo = subRepo;
  }

  public EventService(EventRepository eventRepository, HackathonRepository hackathonRepository){
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
    event.setCreatedByUserId(getCurrentAdminId());
    event.setStatus(calculateLifecycleStatus(event, OffsetDateTime.now(ZoneOffset.UTC), req.getStatus()));
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

    if(req.getVisibility() != null && !ALLOWED_VISIBILITIES.contains(req.getVisibility())){
      throw new IllegalArgumentException("Visibility must be one of "+ALLOWED_VISIBILITIES);
    }

    if(req.getStatus() != null){
      assertValidStatusTransition(event.getStatus(), req.getStatus());
    }

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
      if(!ALLOWED_STATUSES.contains(visibility)){
        throw new IllegalArgumentException("Visibility must be one of "+ALLOWED_STATUSES);
      }
      event.setVisibility(visibility);
    }
    if (status != null) {
      assertValidStatusTransition(event.getStatus(), status);
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

  private void assertValidStatusTransition(String currStat, String newStat){
    if(!ALLOWED_STATUSES.contains(newStat)){
      throw new IllegalArgumentException("New status must be one of "+ALLOWED_STATUSES);
    }
    if(currStat != null && TERMINAL_STATUSES.contains(currStat) && !newStat.equals(currStat)){
      throw new IllegalArgumentException("Event is " + currStat+" it cant change to "+newStat);
    }
  }

  private String calculateLifecycleStatus(Event event, OffsetDateTime now, String reqStatus){
    if("CANCELED".equals(reqStatus)){
      return "CANCELED";
    }
    OffsetDateTime start = event.getStartDateTime();
    OffsetDateTime end = start.plusSeconds(event.getDuration());
    if(!now.isBefore(end)){
      return "COMPLETED";
    }
    if(!now.isBefore(start)){
      return "ACTIVE";
    }
    return "UPCOMING";
  }

  private void validateEventReq(EventRequest req, boolean creating){
    if(req == null){
      throw new IllegalArgumentException("Event request body cant be null");
    }
    if(creating && req.getHackathonId() == null){
      throw new IllegalArgumentException("HackathonId is required");
    }
    if(creating || req.getName() != null){
      if(req.getName() == null || req.getName().isBlank()){
        throw new IllegalArgumentException("Name is required");
      }
    }
    if(creating || req.getTeamSizeLimit() >0){
      if(req.getTeamSizeLimit() <= 0){
        throw new IllegalArgumentException("Team size limit must be greater than 0");
      }
    }
    if(creating || req.getStartDateTime() != null){
      if(req.getStartDateTime() == null){
        throw new IllegalArgumentException("Start date is required");
      }
    }
    if(creating || req.getDuration() >0){
      if(req.getDuration() <=0){
        throw new IllegalArgumentException("Duration is less than 0");
      }
    }
    if(req.getVisibility() != null && !ALLOWED_VISIBILITIES.contains(req.getVisibility())){
      throw new IllegalArgumentException("Visibility must be one of these: "+ALLOWED_VISIBILITIES);
    }
    if(req.getStatus() != null && !ALLOWED_STATUSES.contains(req.getStatus())){
      throw new IllegalArgumentException("Status must be one of these: "+ALLOWED_STATUSES);
    }
    if(creating && req.getVisibility() == null){
      throw new IllegalArgumentException("Visibility is required");
    }
    if("PRIVATE".equals(req.getVisibility()) && (req.getRegistrationKey() == null || req.getRegistrationKey().isBlank())){
      throw new IllegalArgumentException("Registration key is required for private events");
    }
  }
}
