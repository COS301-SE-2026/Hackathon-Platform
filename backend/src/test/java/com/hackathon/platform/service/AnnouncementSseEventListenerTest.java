package com.hackathon.platform.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnnouncementSseEventListenerTest {
    @Test
    void handleAnnouncementCreated_forwardsEvent() {
        AnnouncementUpdateService upSer = mock(AnnouncementUpdateService.class);
        AnnouncementSseEventListener listener = new AnnouncementSseEventListener(upSer);

        UUID eventId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        AnnouncementCreatedEvent event = new AnnouncementCreatedEvent(eventId, messageId);
        listener.handleAnnouncementCreated(event);
        verify(upSer).pushAnnouncementUpdate(eventId, messageId);
    }
}