package com.hackathon.platform.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ForumSseEventListenerTest {
    private ForumUpdateService fuService;
    private ForumSseEventListener listener;

    @BeforeEach
    void setUp() {
        fuService = mock(ForumUpdateService.class);
        listener = new ForumSseEventListener(fuService);
    }

    @Test
    void handleForumUpdated_forwardsEvent() {
        UUID eventId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        ForumUpdatedEvent event = new ForumUpdatedEvent(eventId, "POST_CREATED", resourceId);

        listener.handleForumUpdated(event);
        verify(fuService).pushForumUpdate(eventId, "POST_CREATED", resourceId);
    }
}