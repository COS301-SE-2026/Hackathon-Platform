package com.hackathon.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.ide.TelemetryBatchRequest;
import com.hackathon.platform.ide.TelemetryEventRequest;
import com.hackathon.platform.model.IdeTelemetryEvent;
import com.hackathon.platform.model.IdeTelemetrySession;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.IdeTelemetryEventRepository;
import com.hackathon.platform.repository.IdeTelemetrySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceTelemetryService {
    private final IdeTelemetrySessionRepository sessionRepo;
    private final IdeTelemetryEventRepository eventRepo;
    private final CodeWorkspaceService codeService;
    private final ObjectMapper objMapper;

    @Transactional
    public UUID startSession(UUID workspaceId, User user) {
        var workspace = codeService.getWorkspaceForUser(workspaceId, user);
        LocalDateTime now = LocalDateTime.now();
        IdeTelemetrySession session = IdeTelemetrySession.builder().sessionId(UUID.randomUUID()).workspaceId(workspace.getWorkspaceId()).userId(user.getUserId()).startedAt(now).lastSeenAt(now).build();
        sessionRepo.save(session);
        return session.getSessionId();
    }

    @Transactional
    public void saveBatch(UUID workspaceId, TelemetryBatchRequest req, User user) {
        if (req == null) {
            throw new IllegalArgumentException("batch is required");
        }

        IdeTelemetrySession session = requireValidSession(workspaceId, req.sessionId(), user);

        List<TelemetryEventRequest> reqs = req.events();

        if (reqs == null || reqs.isEmpty()) {
            return;
        }

        if (reqs.size() > 100) {
            throw new IllegalArgumentException("Telemetry batch cannot exceed 100 events");
        }

        LocalDateTime now = LocalDateTime.now();

        List<IdeTelemetryEvent> events = new ArrayList<>();

        for (TelemetryEventRequest reqEvent: reqs) {
            validateEvent(reqEvent);
            IdeTelemetryEvent event = IdeTelemetryEvent.builder().eventId(UUID.randomUUID()).sessionId(session.getSessionId()).workspaceId(workspaceId).userId(user.getUserId()).eventType(reqEvent.eventType().trim()).clientTimestamp(reqEvent.clientTimestamp()).receivedAt(now).sequenceNumber(reqEvent.sequenceNumber()).payload(safePayload(reqEvent.payload())).build();
            events.add(event);
        }

        eventRepo.saveAll(events);
        session.setLastSeenAt(now);
        sessionRepo.save(session);
    }

    @Transactional
    public void endSession(UUID workspaceId, UUID sessionId, User user) {
        IdeTelemetrySession session = requireValidSession(workspaceId, sessionId, user);
        
        LocalDateTime now = LocalDateTime.now();
        session.setLastSeenAt(now);

        if (session.getEndedAt() == null) {
            session.setEndedAt(now);
        }

        sessionRepo.save(session);
    }

    private IdeTelemetrySession requireValidSession(UUID workspaceId, UUID sessionId, User user) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Telemetry session ID is required");
        }

        codeService.getWorkspaceForUser(workspaceId, user);

        IdeTelemetrySession session = sessionRepo.findById(sessionId).orElseThrow( () -> new IllegalArgumentException("Session could not be found"));

        if (!session.getWorkspaceId().equals(workspaceId) || !session.getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("Session does not belong to this user and workspace");
        }

        return session;
    }

    private void validateEvent(TelemetryEventRequest event) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }

        if (event.eventType() == null || event.eventType().isBlank()) {
            throw new IllegalArgumentException("event type is required");
        }

        if (event.eventType().length() > 50) {
            throw new IllegalArgumentException("event type cannot exceed 50 characters");
        }

        if (event.clientTimestamp() == null) {
            throw new IllegalArgumentException("timestamp is required");
        }

        if (event.sequenceNumber() <= 0) {
            throw new IllegalArgumentException("sequence number must be greater than 0");
        }
    }

    private JsonNode safePayload(JsonNode payload) {
        if (payload == null) {
            return objMapper.createObjectNode();
        }

        return payload;
    }
}