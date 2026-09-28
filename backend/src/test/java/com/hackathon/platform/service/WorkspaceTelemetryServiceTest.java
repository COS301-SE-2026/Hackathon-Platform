package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.ide.TelemetryBatchRequest;
import com.hackathon.platform.ide.TelemetryEventRequest;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.service.CodeWorkspaceService;
import com.hackathon.platform.model.IdeTelemetryEvent;
import com.hackathon.platform.model.IdeTelemetrySession;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.IdeTelemetryEventRepository;
import com.hackathon.platform.repository.IdeTelemetrySessionRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class WorkspaceTelemetryServiceTest {
    @Mock private IdeTelemetrySessionRepository sessionRepo;
    @Mock private IdeTelemetryEventRepository eventRepo;
    @Mock private CodeWorkspaceService codeService;
    @Captor private ArgumentCaptor<List<IdeTelemetryEvent>> eventsCaptor;
    private WorkspaceTelemetryService service;

    private final UUID workspaceId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final User user = User.builder().userId(UUID.randomUUID()).build();

    @BeforeEach
    void setUp() {
        service = new WorkspaceTelemetryService(sessionRepo, eventRepo, codeService, new ObjectMapper());
    }

    private IdeTelemetrySession openSession() {
        LocalDateTime now = LocalDateTime.now();
        return IdeTelemetrySession.builder().sessionId(sessionId).workspaceId(workspaceId).userId(user.getUserId()).startedAt(now).lastSeenAt(now).build();
    }

    private TelemetryEventRequest validEvent() {
        return new TelemetryEventRequest ("PASTE", LocalDateTime.now(), 1, null);
    }

    private void saveBatchThrows(TelemetryEventRequest event, String message) {
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(openSession()));
        TelemetryBatchRequest req = new TelemetryBatchRequest(sessionId, Arrays.asList(event));
        
        assertThatThrownBy(() -> service.saveBatch(workspaceId, req, user)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(message);
    }

    @Test
    void startSession_closesOpenSessionsAndCreatesNewOne() {
        CodeWorkspace workspace = mock(CodeWorkspace.class);
        when(workspace.getWorkspaceId()).thenReturn(workspaceId);
        when(codeService.getWorkspaceForUser(workspaceId, user)).thenReturn(workspace);
        IdeTelemetrySession old = openSession();
        when(sessionRepo.findByWorkspaceIdAndUserIdAndEndedAtIsNull(workspaceId, user.getUserId())).thenReturn(List.of(old));
        UUID newId = service.startSession(workspaceId, user);
        
        assertThat(old.getEndedAt()).isNotNull();
        assertThat(newId).isNotNull().isNotEqualTo(sessionId);
        ArgumentCaptor<IdeTelemetrySession> saved = ArgumentCaptor.forClass(IdeTelemetrySession.class);
        verify(sessionRepo).save(saved.capture());
        assertThat(saved.getValue().getSessionId()).isEqualTo(newId);
        assertThat(saved.getValue().getWorkspaceId()).isEqualTo(workspaceId);
    }

    @Test
    void saveBatch_savedEventsAndUpdatesSession() {
        IdeTelemetrySession session = openSession();
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        TelemetryEventRequest event = new TelemetryEventRequest(" PASTE ", LocalDateTime.now(), 1, null);
        service.saveBatch(workspaceId, new TelemetryBatchRequest(sessionId, List.of(event)), user);

        verify(eventRepo).saveAll(eventsCaptor.capture());
        IdeTelemetryEvent saved = eventsCaptor.getValue().get(0);
        assertThat(saved.getEventType()).isEqualTo("PASTE");
        assertThat(saved.getSessionId()).isEqualTo(sessionId);
        verify(sessionRepo).save(session);
    }

    @Test
    void saveBatch_throws_whenRequestNull() {
        assertThatThrownBy(() -> service.saveBatch(workspaceId, null, user)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveBatch_doesNothing_whenNoEvents() {
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(openSession()));
        service.saveBatch(workspaceId, new TelemetryBatchRequest(sessionId, List.of()), user);
        verifyNoInteractions(eventRepo);
    }

    @Test
    void saveBatch_throws_whenSessionIdNull() {
        TelemetryBatchRequest req = new TelemetryBatchRequest(null, List.of(validEvent()));
        assertThatThrownBy(() -> service.saveBatch(workspaceId, req, user)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveBatch_throws_whenSessionNotFound() {
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.empty());
        TelemetryBatchRequest req = new TelemetryBatchRequest(sessionId, List.of(validEvent()));
        assertThatThrownBy(() -> service.saveBatch(workspaceId, req, user)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveBatch_throws_whenSessionBelongsToSomeoneElse() {
        IdeTelemetrySession session = openSession();
        session.setUserId(UUID.randomUUID());
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        TelemetryBatchRequest req = new TelemetryBatchRequest(sessionId, List.of(validEvent()));

        assertThatThrownBy(() -> service.saveBatch(workspaceId, req, user)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void saveBatch_throws_whenMoreThan100Events() {
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(openSession()));
        List<TelemetryEventRequest> events = Arrays.asList(new TelemetryEventRequest[101]);
        TelemetryBatchRequest req = new TelemetryBatchRequest(sessionId, events);

        assertThatThrownBy(() -> service.saveBatch(workspaceId, req, user)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveBatch_throws_whenEventNull() {
        saveBatchThrows(null, "event cannot be null");
    }

    @Test
    void saveBatch_throws_whenEventTypeBlank() {
        saveBatchThrows(new TelemetryEventRequest("x".repeat(51), LocalDateTime.now(), 1, null), "50 characters");
    }

    @Test
    void saveBatch_throws_whenTimestampMissing() {
        saveBatchThrows(new TelemetryEventRequest("PASTE", null, 1, null), "timestamp");
    }

    @Test
    void saveBatch_throws_whenSequenceNumberNotPositive() {
        saveBatchThrows(new TelemetryEventRequest("PASTE", LocalDateTime.now(), 0, null), "sequence number");
    }

    @Test
    void endSession_setsEndedAtAndSaves() {
        IdeTelemetrySession session = openSession();
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        service.endSession(workspaceId, sessionId, user);
        assertThat(session.getEndedAt()).isNotNull();
        verify(sessionRepo).save(session);
    }

    @Test
    void endSession_keepsExistingEndedAt() {
        IdeTelemetrySession session = openSession();
        LocalDateTime earlier = LocalDateTime.now().minusMinutes(5);
        session.setEndedAt(earlier);
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        service.endSession(workspaceId, sessionId, user);
        assertThat(session.getEndedAt()).isEqualTo(earlier);
    }
}