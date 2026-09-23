package com.hackathon.platform.service;

import com.hackathon.platform.model.IdeTelemetryEvent;
import com.hackathon.platform.repository.IdeTelemetryEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelemetryFeatureService {
    private static final int LARGE_PASTE_CHARACTERS = 100;
    private static final long PASTE_AFTER_RETURN_SECONDS = 5;
    private final IdeTelemetryEventRepository eventRepo;

    @Transactional(readOnly = true)
    public TelemetryFeatures extractFeatures(UUID workspaceId, UUID userId) {
        List<IdeTelemetryEvent> events = eventRepo.findByWorkspaceIdAndUserId(workspaceId, userId);

        if (events.isEmpty()) {
            return TelemetryFeatures.empty();
        }

        Map<UUID, List<IdeTelemetryEvent>> eventsBySession = new HashMap<>();
        for (IdeTelemetryEvent event : events) {
            eventsBySession.computeIfAbsent(event.getSessionId(), ignored -> new ArrayList<>()).add(event);
        }

        FeatureAccumulator accumulator = new FeatureAccumulator();
        accumulator.eventCount = events.size();
        accumulator.sessionCount = eventsBySession.size();

        for (List<IdeTelemetryEvent> sessionEvents : eventsBySession.values()) {
            sessionEvents.sort(Comparator.comparingLong(IdeTelemetryEvent::getSequenceNumber));
            processSession(sessionEvents, accumulator);
        }

        long totalInsertedCharacters = accumulator.typedCharacters + accumulator.pastedCharacters;
        double pasteFraction = ratio(accumulator.pastedCharacters, totalInsertedCharacters);
        double reworkRatio = ratio(accumulator.deletedCharacters, totalInsertedCharacters);
        double avgTypingInterval = 0;
        if (accumulator.typingIntervalWeight > 0) {
            avgTypingInterval = accumulator.typingIntervalWeightedTotal / accumulator.typingIntervalWeight;
        }
        return new TelemetryFeatures(
            accumulator.eventCount,
            accumulator.sessionCount,
            accumulator.typedCharacters,
            accumulator.typingEdits,
            accumulator.typingBatchCount,
            avgTypingInterval,
            accumulator.pastedCharacters,
            accumulator.pasteEvents,
            accumulator.largePasteCount,
            accumulator.largestPasteCharacters,
            accumulator.largestPasteLines,
            pasteFraction,
            accumulator.deletedCharacters,
            accumulator.deletionEdits,
            reworkRatio,
            accumulator.focusLostCount,
            accumulator.focusGainedCount,
            accumulator.tabHiddenCount,
            accumulator.tabVisibleCount,
            accumulator.totalTabAwaySeconds,
            accumulator.maxTabAwaySeconds,
            accumulator.pasteAfterTabReturnCount,
            accumulator.largePasteAfterTabReturnCount,
            accumulator.runCount,
            accumulator.successfulRuns,
            accumulator.codeErrorRuns,
            accumulator.requestErrorRuns,
            accumulator.submitCount,
            accumulator.fileOpenedCount,
            accumulator.activeDurationSeconds
        );
    }

    private void processSession(List<IdeTelemetryEvent> events, FeatureAccumulator accumulator) {
        if (events.isEmpty()) {
            return;
        }

        LocalDateTime firstTimestamp = null;
        LocalDateTime lastTimestamp = null;
        LocalDateTime tabHiddenAt = null;
        LocalDateTime lastTabVisibleAt = null;

        for (IdeTelemetryEvent event : events) {
            LocalDateTime timestamp = event.getClientTimestamp();
            if (timestamp != null) {
                if (firstTimestamp == null || timestamp.isBefore(firstTimestamp)) {
                    firstTimestamp = timestamp;
                }

            if (lastTimestamp == null || timestamp.isAfter(lastTimestamp)) {
                lastTimestamp = timestamp;
            }
        }

        String eventType = event.getEventType();

        if (eventType == null) {
            continue;
        }

        switch (eventType) {
            case "FILE_OPENED" -> accumulator.fileOpenedCount++;
            case "TYPING_BATCH" -> {
                long characters = longPayload(event, "characters");
                long edits = longPayload(event, "edits");
                double averageInterval = doublePayload(event, "avgIntervalMs");

                accumulator.typedCharacters += characters;
                accumulator.typingEdits += edits;
                accumulator.typingBatchCount++;
                long intervalWeight = Math.max(0, edits - 1);
                accumulator.typingIntervalWeightedTotal += averageInterval * intervalWeight;
                accumulator.typingIntervalWeight += intervalWeight;
            }

            case "PASTE" -> {
                long characters = longPayload(event, "characters");
                long lines = longPayload(event, "lines");
                accumulator.pastedCharacters += characters;
                accumulator.pasteEvents++;
                accumulator.largestPasteCharacters = Math.max(accumulator.largestPasteCharacters, characters);
                accumulator.largestPasteLines = Math.max(accumulator.largestPasteLines, lines);
                if (characters >= LARGE_PASTE_CHARACTERS) {
                    accumulator.largePasteCount++;
                }

                if (timestamp != null && lastTabVisibleAt != null) {
                    long secondsSinceReturn = Duration.between(lastTabVisibleAt, timestamp).getSeconds();

                    if (secondsSinceReturn >= 0 && secondsSinceReturn <= PASTE_AFTER_RETURN_SECONDS) {
                        accumulator.pasteAfterTabReturnCount++;

                        if (characters >= LARGE_PASTE_CHARACTERS) {
                            accumulator.largePasteAfterTabReturnCount++;
                        }
                    }
                }
            }

            case "DELETE" -> {
                accumulator.deletedCharacters += longPayload(event, "characters");
                accumulator.deletionEdits += longPayload(event, "edits");
            }

            case "FOCUS_LOST" -> accumulator.focusLostCount++;
            case "FOCUS_GAINED" -> accumulator.focusGainedCount++;
            case "TAB_HIDDEN" -> {
                accumulator.tabHiddenCount++;

                if (timestamp != null && tabHiddenAt == null) {
                    tabHiddenAt = timestamp;
                }
            }

            case "TAB_VISIBLE" -> {
                accumulator.tabVisibleCount++;
                if (timestamp != null) {
                    lastTabVisibleAt = timestamp;

                    if (tabHiddenAt != null) {
                        long awaySeconds = Duration.between(tabHiddenAt, timestamp).getSeconds();

                        if (awaySeconds >= 0) {
                            accumulator.totalTabAwaySeconds += awaySeconds;
                            accumulator.maxTabAwaySeconds = Math.max(accumulator.maxTabAwaySeconds, awaySeconds);
                        }
                        tabHiddenAt = null;
                    }
                }
            }

            case "RUN" -> accumulator.runCount++;
            case "RUN_RESULT" -> {
                String res = textPayload(event, "result");

                switch (res) {
                    case "SUCCESS" -> accumulator.successfulRuns++;
                    case "CODE_ERROR" -> accumulator.codeErrorRuns++;
                    case "REQUEST_ERROR" -> accumulator.requestErrorRuns++;
                    default -> {}
                }
            }

            case "SUBMIT" -> accumulator.submitCount++;
            default -> {}
        }
    }

    if (firstTimestamp != null && lastTimestamp != null && !lastTimestamp.isBefore(firstTimestamp)) {
        accumulator.activeDurationSeconds += Duration.between(firstTimestamp, lastTimestamp).getSeconds();
    }
}

        private long longPayload(IdeTelemetryEvent event, String field) {
            if (event.getPayload() == null || !event.getPayload().has(field)) {
                return 0;
            }

            return event.getPayload().path(field).asLong(0);
        }

        private double doublePayload(IdeTelemetryEvent event, String field) {
            if (event.getPayload() == null || !event.getPayload().has(field)) {
                return 0;
            }

            return event.getPayload().path(field).asDouble(0);
        }

        private String textPayload(IdeTelemetryEvent event, String field) {
            if (event.getPayload() == null || !event.getPayload().has(field)) {
                return "";
            }

            return event.getPayload().path(field).asText("");
        }

        private double ratio(long numerator, long denominator) {
            if (denominator <= 0) {
                return 0;
            }

            return (double) numerator / denominator;
        }

        private static class FeatureAccumulator {
            long eventCount;
            long sessionCount;
            long typedCharacters;
            long typingEdits;
            long typingBatchCount;
            double typingIntervalWeightedTotal;
            long typingIntervalWeight;
            long pastedCharacters;
            long pasteEvents;
            long largePasteCount;
            long largestPasteCharacters;
            long largestPasteLines;
            long deletedCharacters;
            long deletionEdits;
            long focusLostCount;
            long focusGainedCount;
            long tabHiddenCount;
            long tabVisibleCount;
            long totalTabAwaySeconds;
            long maxTabAwaySeconds;
            long pasteAfterTabReturnCount;
            long largePasteAfterTabReturnCount;
            long runCount;
            long successfulRuns;
            long codeErrorRuns;
            long requestErrorRuns;
            long submitCount;
            long fileOpenedCount;
            long activeDurationSeconds;
        }

        public record TelemetryFeatures(
            long eventCount,
            long sessionCount,
            long typedCharacters,
            long typingEdits,
            long typingBatchCount,
            double averageTypingIntervalMs,
            long pastedCharacters,
            long pasteEvents,
            long largePasteCount,
            long largestPasteCharacters,
            long largestPasteLines,
            double pasteFraction,
            long deletedCharacters,
            long deletionEdits,
            double reworkRatio,
            long focusLostCount,
            long focusGainedCount,
            long tabHiddenCount,
            long tabVisibleCount,
            long totalTabAwaySeconds,
            long maxTabAwaySeconds,
            long pasteAfterTabReturnCount,
            long largePasteAfterTabReturnCount,
            long runCount,
            long successfulRuns,
            long codeErrorRuns,
            long requestErrorRuns,
            long submitCount,
            long fileOpenedCount,
            long activeDurationSeconds
        ) {
        public static TelemetryFeatures empty() {
            return new TelemetryFeatures(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}