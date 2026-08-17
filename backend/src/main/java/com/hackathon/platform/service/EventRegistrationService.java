package com.hackathon.platform.service;

import com.hackathon.platform.dto.EventRegistrationResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.EventRegistration;
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.repository.EventRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EventRegistrationService {

  private final EventRepository eventRepo;
  private final EventRegistrationRepository eventRegistrationRepo;

  public EventRegistrationService(
      EventRepository eventRepo, EventRegistrationRepository eventRegistrationRepo) {
    this.eventRepo = eventRepo;
    this.eventRegistrationRepo = eventRegistrationRepo;
  }

  @Transactional
  public EventRegistrationResponse registerForEvent(UUID eventId, UUID userId, String key) {
    Event event =
        eventRepo.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));
    if ("COMPLETED".equals(event.getStatus()) || "CANCELLED".equals(event.getStatus())) {
      throw new RuntimeException("This event is not accepting registrations");
    }

    if ("PRIVATE".equals(event.getVisibility())) {
      if (key == null || key.isBlank() || !key.equals(event.getRegistrationKey())) {
        throw new RuntimeException("Registration key is not correct");
      }
    }

    if (eventRegistrationRepo.existsByEventIdAndUserId(eventId, userId)) {
      throw new RuntimeException("You're already registered for this event");
    }

    EventRegistration reg = new EventRegistration();
    reg.setEventId(eventId);
    reg.setUserId(userId);
    EventRegistration save = eventRegistrationRepo.save(reg);

    return toResponse(save);
  }

  public boolean isRegistered(UUID event, UUID user) {
    return eventRegistrationRepo.existsByEventIdAndUserId(event, user);
  }

  public List<EventRegistrationResponse> getMyRegistrations(UUID user) {
    return eventRegistrationRepo.findByUserId(user).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  private EventRegistrationResponse toResponse(EventRegistration reg) {
    return new EventRegistrationResponse(
        reg.getRegistrationId(), reg.getEventId(), reg.getRegisteredAt());
  }
}
