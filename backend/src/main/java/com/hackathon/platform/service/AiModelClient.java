package com.hackathon.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.Optional;

@Service
@Slf4j
public class AiModelClient {
    private final RestClient rest;

    public AiModelClient(@Value("${ai.model.base-url:http://localhost:8001}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Optional<AiPredictionResponse> predict(TelemetryFeatureService.TelemetryFeatures features) {
        AiPredictionRequest req = new AiPredictionRequest(
            features.typedCharacters(),
            features.typingEdits(),
            features.typingBatchCount(),
            features.averageTypingIntervalMs(),
            features.pastedCharacters(),
            features.pasteEvents(),
            features.largePasteCount(),
            features.largestPasteCharacters(),
            features.largestPasteLines(),
            features.pasteFraction(),
            features.deletedCharacters(),
            features.deletionEdits(),
            features.reworkRatio(),
            features.focusLostCount(),
            features.focusGainedCount(),
            features.tabHiddenCount(),
            features.tabVisibleCount(),
            features.totalTabAwaySeconds(),
            features.maxTabAwaySeconds(),
            features.pasteAfterTabReturnCount(),
            features.largePasteAfterTabReturnCount(),
            features.runCount(),
            features.successfulRuns(),
            features.codeErrorRuns(),
            features.requestErrorRuns(),
            features.submitCount(),
            features.fileOpenedCount(),
            features.activeDurationSeconds()
        );

        try {
            AiPredictionResponse res = rest.post().uri("/predict").body(req).retrieve().body(AiPredictionResponse.class);
            return Optional.ofNullable(res);
        } catch (RestClientException exception) {
            log.warn("request failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    public record AiPredictionRequest(
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
    ) {}

    public record AiPredictionResponse(
        String modelVersion,
        double aiAssistanceLikelihood,
        double aiAssistanceLikelihoodPercent,
        double reviewThreshold,
        boolean flaggedForReview
    ) {}
}