package com.hackathon.platform.service;

import com.hackathon.platform.model.Event;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.TeamMemberRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnnouncementAccessService {
  private final EventRepository eventRepo;
  private final TeamMemberRepository teamMemberRepo;

  public Event requireEventOwner(UUID eventId, UUID adminUserId) {
    Event event =
        eventRepo
            .findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
    if (!adminUserId.equals(event.getCreatedByUserId())) {
      throw new AccessDeniedException(adminUserId + " is not the admin of this event");
    }
    return event;
  }

  public void requireParticipantAccess(UUID eventId, UUID userId) {
    if (!eventRepo.existsById(eventId)) {
      throw new IllegalArgumentException("Event could not be found: " + eventId);
    }

    boolean access = teamMemberRepo.existsApprovedParticipantInEvent(userId, eventId);

    if (!access) {
      throw new AccessDeniedException("You are not a participant in this event");
    }
  }
}
