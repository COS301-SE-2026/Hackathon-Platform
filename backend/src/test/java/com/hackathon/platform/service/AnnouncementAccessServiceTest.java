package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.EventRegistrationRepository;

@ExtendWith(MockitoExtension.class)
class AnnouncementAccessServiceTest {
    @Mock private EventRepository eventRepo;
    @Mock private EventRegistrationRepository eventRegRepo;
    @InjectMocks private AnnouncementAccessService accSer;

    private UUID eventId;
    private UUID adminId;
    private UUID userId;
    private Event event;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();
        event = new Event();
        event.setEventId(eventId);
        event.setCreatedByUserId(adminId);
    }

    @Test
    void requireEventOwner_owner_returnsEvent() {
        when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
        assertThat(accSer.requireEventOwner(eventId, adminId));
    }

    @Test
    void requireEventOwner_eventNotFound_throwsException() {
        when(eventRepo.findById(eventId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> accSer.requireEventOwner(eventId, adminId)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireEventOwner_notOwner_throwsAccessDenied() {
        when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
        assertThatThrownBy(() -> accSer.requireEventOwner(eventId, userId)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireParticipantAccess_registered_success() {
        when(eventRepo.existsById(eventId)).thenReturn(true);
        when(eventRegRepo.existsByEventIdAndUserId(eventId, userId)).thenReturn(true);
        accSer.requireParticipantAccess(eventId, userId);
        verify(eventRegRepo).existsByEventIdAndUserId(eventId, userId);
    }

    @Test
    void requireParticipantAccess_eventNotFound_throwsException() {
        when(eventRepo.existsById(eventId)).thenReturn(false);
        assertThatThrownBy(() -> accSer.requireParticipantAccess(eventId, userId)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireParticipantAccess_notRegistered_throwsAccessDenied() {
        when(eventRepo.existsById(eventId)).thenReturn(true);
        when(eventRegRepo.existsByEventIdAndUserId(eventId, userId)).thenReturn(false);
        assertThatThrownBy(() -> accSer.requireParticipantAccess(eventId, userId)).isInstanceOf(AccessDeniedException.class);
    }
}
