package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackathon.platform.model.IdeTelemetryEvent;
import com.hackathon.platform.repository.IdeTelemetryEventRepository;
import com.hackathon.platform.service.TelemetryFeatureService.TelemetryFeatures;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelemetryFeatureServiceTest {
  @Mock private IdeTelemetryEventRepository eventRepo;
  private TelemetryFeatureService service;

  private final ObjectMapper mapper = new ObjectMapper();
  private final UUID workspaceId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID sessionId = UUID.randomUUID();
  private final LocalDateTime start = LocalDateTime.of(2026, 1, 1, 12, 0, 0);

  @BeforeEach
  void setUp() {
    service = new TelemetryFeatureService(eventRepo);
  }

  private ObjectNode json() {
    return mapper.createObjectNode();
  }

  private IdeTelemetryEvent event(
      UUID session, String type, long seq, int secondsAfterStart, ObjectNode payload) {
    return IdeTelemetryEvent.builder()
        .sessionId(session)
        .eventType(type)
        .sequenceNumber(seq)
        .clientTimestamp(start.plusSeconds(secondsAfterStart))
        .payload(payload)
        .build();
  }

  private IdeTelemetryEvent event(
      String type, long seq, int secondsAfterStart, ObjectNode payload) {
    return event(sessionId, type, seq, secondsAfterStart, payload);
  }

  private TelemetryFeatures extract(IdeTelemetryEvent... events) {
    when(eventRepo.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(List.of(events));
    return service.extractFeatures(workspaceId, userId);
  }

  @Test
  void extractFeatures_returnsEmpty_whenNoEvents() {
    TelemetryFeatures features = extract();

    assertThat(features).isEqualTo(TelemetryFeatures.empty());
    assertThat(features.eventCount()).isZero();
  }

  @Test
  void extractFeatures_calculatesTypingPasteAndDeleteTotals() {
    TelemetryFeatures features =
        extract(
            event(
                "TYPING_BATCH",
                1,
                0,
                json().put("characters", 80).put("edits", 5).put("avgIntervalMs", 100)),
            event("PASTE", 2, 30, json().put("characters", 20).put("lines", 2)),
            event("DELETE", 3, 60, json().put("characters", 10).put("edits", 2)));

    assertThat(features.eventCount()).isEqualTo(3);
    assertThat(features.sessionCount()).isEqualTo(1);
    assertThat(features.typedCharacters()).isEqualTo(80);
    assertThat(features.typingEdits()).isEqualTo(5);
    assertThat(features.typingBatchCount()).isEqualTo(1);
    assertThat(features.averageTypingIntervalMs()).isCloseTo(100.0, within(0.001));
    assertThat(features.pastedCharacters()).isEqualTo(20);
    assertThat(features.pasteEvents()).isEqualTo(1);
    assertThat(features.largestPasteLines()).isEqualTo(2);
    assertThat(features.deletedCharacters()).isEqualTo(10);
    assertThat(features.deletionEdits()).isEqualTo(2);
    assertThat(features.pasteFraction()).isCloseTo(0.2, within(0.001));
    assertThat(features.reworkRatio()).isCloseTo(0.1, within(0.001));
    assertThat(features.activeDurationSeconds()).isEqualTo(60);
  }

  @Test
  void extractFeatures_flagsLargePasteRightAfterReturningToTab() {
    TelemetryFeatures features =
        extract(
            event("TAB_HIDDEN", 1, 0, json()),
            event("TAB_VISIBLE", 2, 20, json()),
            event("PASTE", 3, 22, json().put("characters", 150)));

    assertThat(features.tabHiddenCount()).isEqualTo(1);
    assertThat(features.tabVisibleCount()).isEqualTo(1);
    assertThat(features.totalTabAwaySeconds()).isEqualTo(20);
    assertThat(features.maxTabAwaySeconds()).isEqualTo(20);
    assertThat(features.largePasteCount()).isEqualTo(1);
    assertThat(features.largestPasteCharacters()).isEqualTo(150);
    assertThat(features.pasteAfterTabReturnCount()).isEqualTo(1);
    assertThat(features.largePasteAfterTabReturnCount()).isEqualTo(1);
  }

  @Test
  void extractFeatures_countsRunsFocusSubmitsAndSessions() {
    UUID otherSession = UUID.randomUUID();

    TelemetryFeatures features =
        extract(
            event("FILE_OPENED", 1, 0, json()),
            event("RUN", 2, 1, json()),
            event("RUN_RESULT", 3, 2, json().put("result", "SUCCESS")),
            event("RUN_RESULT", 4, 3, json().put("result", "CODE_ERROR")),
            event("RUN_RESULT", 5, 4, json().put("result", "REQUEST_ERROR")),
            event("RUN_RESULT", 6, 5, json().put("result", "SOMETHING_ELSE")),
            event("RUN_RESULT", 7, 6, null),
            event("SUBMIT", 8, 7, json()),
            event("UNKNOWN_TYPE", 9, 8, json()),
            event(null, 10, 9, json()),
            event(otherSession, "FOCUS_LOST", 1, 0, json()),
            event(otherSession, "FOCUS_GAINED", 2, 5, json()));

    assertThat(features.sessionCount()).isEqualTo(2);
    assertThat(features.fileOpenedCount()).isEqualTo(1);
    assertThat(features.runCount()).isEqualTo(1);
    assertThat(features.successfulRuns()).isEqualTo(1);
    assertThat(features.codeErrorRuns()).isEqualTo(1);
    assertThat(features.requestErrorRuns()).isEqualTo(1);
    assertThat(features.submitCount()).isEqualTo(1);
    assertThat(features.focusLostCount()).isEqualTo(1);
    assertThat(features.focusGainedCount()).isEqualTo(1);
  }
}
