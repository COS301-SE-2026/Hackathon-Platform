package com.hackathon.platform.service;

import com.hackathon.platform.model.Event;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.dto.EventStatusResponse;
import com.hackathon.platform.dto.EventRequest;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.List;

@Service
public class EventService {
    private final EventRepository eventRepository;
    public static final String CREATEDUSER = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11";

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Event createEvent(EventRequest req) {
        Event event = new Event();
        event.setCreatedByUserId(UUID.fromString(CREATEDUSER));
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

    public List<Event> getEventByCreator(UUID userId) {
        return eventRepository.fetchAllByAdmin(userId);
    }

    public Event putUpdateEvent (UUID eventId, EventRequest req) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));

        event.setCreatedByUserId(UUID.fromString(CREATEDUSER));
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

    public EventStatusResponse getEventStatus(UUID eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));

        return new EventStatusResponse(
            event.getEventId(),
            event.getStatus(),
            event.getVisibility()
        );
    }
}