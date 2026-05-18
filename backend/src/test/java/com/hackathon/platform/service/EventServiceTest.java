package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.model.Event;
import com.hackathon.platform.repository.EventRepository;
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
    @Mock private EventRepository EventRepository;
    @InjectMocks EventService EventService;

    private UUID eventId;
    private Event previousEvent;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        previousEvent = new Event();
        previousEvent.setEventId(eventId);
        previousEvent.setName("Test Hackathon");
        previousEvent.setVibility("PUBLIC");
        previousEvent.setStatus("INACTIVE");
        previousEvent.setRegistrationKey(null);
    }
}