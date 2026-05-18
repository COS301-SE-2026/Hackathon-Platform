package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.model.Event;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.dto.EventRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    @Mock private EventRepository eventRepository;
    @Mock private EventRequest eventRequest;
    @InjectMocks EventService eventService;

    private UUID eventId;
    private UUID creatorUserId;
    private Event event;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        creatorUserId = UUID.randomUUID();
        event = new Event();
        event.setEventId(eventId);
        event.setCreatedByUserId(creatorUserId);
        event.setName("Test Hackathon");
        event.setVisibility("PUBLIC");
        event.setStatus("INACTIVE");
        event.setRegistrationKey(null);
    }

    @Test
    void getEventByCreator_withValidCreatorId_returnsMatchingEvents() {
        when(eventRepository.fetchAllByAdmin(creatorUserId)).thenReturn(Collections.singletonList(event));
        List<Event> results = eventService.getEventByCreator(creatorUserId);

        assertThat(results).isNotEmpty().hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Test Hackathon");
        assertThat(results.get(0).getCreatedByUserId()).isEqualTo(creatorUserId);
        verify(eventRepository).fetchAllByAdmin(creatorUserId);
    }

    @Test
    void getEventByCreator_withInvalidCreatorId_returnsEmptyList() {
        UUID invalid = UUID.randomUUID();
        when(eventRepository.fetchAllByAdmin(invalid)).thenReturn(Collections.emptyList());
        List<Event> results = eventService.getEventByCreator(invalid);

        assertThat(results).isNotNull().isEmpty();
        verify(eventRepository).fetchAllByAdmin(invalid);
    }

    @Test
    void createEvent_withValidPayload_returnsSavedEvent() {
        EventRequest req = new EventRequest();
        req.setName("My new name");
        req.setVisibility("PUBLIC");
        req.setStatus("ACTIVE");
        req.setDescription("This is a test");
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Event result = eventService.createEvent(req);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("My new name");
        assertThat(result.getVisibility()).isEqualTo("PUBLIC");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getDescription()).isEqualTo("This is a test");
        assertThat(result.getCreatedByUserId()).isEqualTo(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void putUpdateEvent_withValidId_returnsUpdatedEvent() {
        EventRequest req = new EventRequest();
        req.setName("My new name");
        req.setVisibility("PRIVATE");
        req.setStatus("ACTIVE");
        req.setDescription("This is a test");
        req.setRegistrationKey("THISISAKEY");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = eventService.putUpdateEvent(eventId, req);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("My new name");
        assertThat(result.getVisibility()).isEqualTo("PRIVATE");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getDescription()).isEqualTo("This is a test");
        assertThat(result.getRegistrationKey()).isEqualTo("THISISAKEY");
        assertThat(result.getCreatedByUserId()).isEqualTo(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));

        verify(eventRepository).findById(eventId);
        verify(eventRepository).save(any(Event.class));
    }
}