package com.hackathon.platform.service;

import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnouncementEmailEventListenerTest {
    @Mock
    private AnnouncementEmailDeliveryService emailService;
    
    @InjectMocks
    private AnnouncementEmailEventListener listener;

    @Test
    void announcementEventProcessesEmailDeliveries() {
        UUID eventId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();
        AnnouncementCreatedEvent event = new AnnouncementCreatedEvent(eventId, msgId);

        listener.handleAnnouncement(event);
        verify(emailService).processMessage(msgId);
    }
}