package com.hackathon.platform.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class LeaderboardUpdateServiceTest {
    private final LeaderboardUpdateService service = new LeaderboardUpdateService();

    private static final UUID EVENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID FIRST_TEAM =UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    @Test
    void subscribe_returnsSseEmitter() {
        SseEmitter emitter = service.subscribe(EVENT_ID);

        assertThat(emitter).isNotNull();
    }

    @Test
    void pushLeaderboardUpdate_WithSubscribers_completeNormally() {
        SseEmitter emitter = service.subscribe(EVENT_ID);
        service.pushLeaderboardUpdate(EVENT_ID, 1L, FIRST_TEAM, 40L);

        assertThat(emitter).isNotNull();
    }

    @Test
    void pushLeaderboardUpdate_forDifferentEvent_completesNormally() {
        UUID otherEvent = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");
        SseEmitter emitter = service.subscribe(otherEvent);
        service.pushLeaderboardUpdate(EVENT_ID, 1L, FIRST_TEAM, 40L);

        assertThat(emitter).isNotNull();
    }
}