package com.hackathon.platform.scoring;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class LeaderboardUpdateService {
    private static final long TIMEOUT_MS = 30L * 60L * 1000L;
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> eventSubscribers = new ConcurrentHashMap<>();
    public SseEmitter subscribe(UUID eventId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        eventSubscribers.computeIfAbsent(eventId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> removeEmitter(eventId, emitter);

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("Connect").data(Map.of("eventId", eventId.toString())));
        } catch (IOException | IllegalStateException exception) {
            removeEmitter(eventId, emitter);
        }

        return emitter;
    }

    public void pushLeaderboardUpdate(UUID eventId, Long levelId, UUID teamId, Long subId) {
        List<SseEmitter> emitters = eventSubscribers.get(eventId);

        if(emitters == null || emitters.isEmpty()) {
            return;
        }

        Map<String, Object> data = Map.of(
            "eventId", eventId.toString(),
            "levelId", levelId,
            "teamId", teamId.toString(),
            "submissionId", subId,
            "occurredAt", Instant.now().toString()
        );

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("leaderboard-update").id(String.valueOf(subId)).data(data));
            } catch (IOException | IllegalStateException exception) {
                removeEmitter(eventId, emitter);
            }
        }
    }

    private void removeEmitter(UUID eventId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = eventSubscribers.get(eventId);

        if(emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if(emitters.isEmpty()) {
            eventSubscribers.remove(eventId, emitters);
        }
    }
}