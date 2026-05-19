package com.hackathon.platform.service;

import com.hackathon.platform.dto.EventRequest;
import com.hackathon.platform.model.User;
import com.hackathon.platform.dto.EventStatusResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.repository.EventRepository;
import java.util.List;
import java.util.UUID;

import javax.management.RuntimeErrorException;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class EventService {
  private final EventRepository eventRepository;
  //public static final String CREATEDUSER = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11";

  public EventService(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  private UUID getCurrentAdminId(){
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return user.getUserId();
  }

  /** create event */
  public Event createEvent(EventRequest req) {
    Event event = new Event();
    event.setCreatedByUserId(getCurrentAdminId());
    event.setName(req.getName());
    event.setRegistrationKey(req.getRegistrationKey());
    event.setTeamSizeLimit(req.getTeamSizeLimit());
    event.setStartDateTime(req.getStartDateTime());
    event.setDuration(req.getDuration());
    event.setDescription(req.getDescription());
    event.setVisibility(req.getVisibility());
    event.setStatus(req.getStatus());

    return eventRepository.save(event);
  }

  /**Return all events created by the current admin */
  public List<Event> getEventsByCurrentAdmin(){
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
  return eventRepository.findByVisibilityAndStatusIn(
      "PUBLIC",
      List.of("UPCOMING", "ONGOING", "ACTIVE")
  );
}
}
