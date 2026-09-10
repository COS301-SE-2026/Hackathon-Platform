package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.EventRegistrationResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.EventRegistration;
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.repository.EventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceTest {
  @Mock private EventRepository eventRepo;
  @Mock private EventRegistrationRepository eventRegistrationRepo;
  @InjectMocks private EventRegistrationService eventRegistrationService;

  private UUID eventId;
  private UUID userId;
  private Event event;

  @BeforeEach
  void setUp() {
    eventId = UUID.randomUUID();
    userId = UUID.randomUUID();

    event = new Event();
    event.setEventId(eventId);
    event.setName("Test Hackathon");
    event.setStatus("ACTIVE");
    event.setVisibility("PUBLIC");
    event.setRegistrationKey(null);
    event.setInPerson(false);

  }

  private EventRegistration buildSavedRegistration() {
    
    EventRegistration reg = new EventRegistration();
    reg.setRegistrationId(UUID.randomUUID());
    reg.setEventId(eventId);
    reg.setUserId(userId);
    reg.setRegisteredAt(Instant.now());
    return reg;

  }

}