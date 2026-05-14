package com.hackathon.platform.service;

import com.hackathon.platform.model.Event;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.dto.EventRequest;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class EventService {
    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Event createEvent(EventRequest req) {
        Event event = new Event();
        event.setCreatedByUserId(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));
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
}