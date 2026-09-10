package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ForumUpdateServiceTest {
    private ForumUpdateService fuService;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        fuService = new ForumUpdateService();
        eventId = UUID.randomUUID();
    }

    @Test
    void subscribe_returnsEmitter() {
        SseEmitter em = fuService.subscribe(eventId);
        assertThat(em).isNotNull();
        assertThat(em.getTimeout()).isEqualTo(30L * 60L * 1000L);
    }

    @Test
    void pushForumUpdate_sendsUpdate() throws Exception {
        SseEmitter em = mock(SseEmitter.class);
        Map<UUID, CopyOnWriteArrayList<SseEmitter>> subs = (Map<UUID, CopyOnWriteArrayList<SseEmitter>>)ReflectionTestUtils.getField(fuService, "eventSubscribers");

        subs.put(eventId, new CopyOnWriteArrayList<>(List.of(em)));
        fuService.pushForumUpdate(eventId, "POST_CREATED", UUID.randomUUID());
        verify(em).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void pushForumUpdate_sendFails_removeEmitter() throws Exception {
        SseEmitter em = mock(SseEmitter.class);
        Map<UUID, CopyOnWriteArrayList<SseEmitter>> subs = (Map<UUID, CopyOnWriteArrayList<SseEmitter>>)ReflectionTestUtils.getField(fuService, "eventSubscribers");
        subs.put(eventId, new CopyOnWriteArrayList<>(List.of(em)));
        
        doThrow(new IOException()).when(em).send(any(SseEmitter.SseEventBuilder.class));
        fuService.pushForumUpdate(eventId, "POST_CREATED", UUID.randomUUID());
        assertThat(subs).doesNotContainKey(eventId);
    }
}
