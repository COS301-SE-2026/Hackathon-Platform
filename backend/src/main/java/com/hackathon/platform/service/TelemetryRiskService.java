package com.hackathon.platform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelemetryRiskService {
    private static final String SCORING_VERSION = "BEHAVIOR_V1";
    private final TelemetryFeatureService featureService;
    private final AiModelClient aiClient;

    public TelemetryRiskReport analyze(UUID workspaceId, UUID userId) {
        TelemetryFeatureService.TelemetryFeatures features = featureService.extractFeatures(workspaceId, userId);

        if (features.eventCount() == 0) {
            return new TelemetryRiskReport(
                SCORING_VERSION,
                0,
                "INSUFFICIENT_DATA",
                0,
                List.of("There is not enough telemetry to perform behavioral analysis"),
                features,
                null
            );
        }

        int riskScore = calcRiskScore(features);
        int evidenceConfidence = calcEvidenceConfidence(features);
        String reviewLevel;

        if (evidenceConfidence < 40) {
            reviewLevel = "INSUFFICIENT_DATA";
        } else {
            reviewLevel = determineReviewLevel(riskScore);
        }

        List<String> indicators = buildIndicators(features);

        if (indicators.isEmpty()) {
            indicators.add("no indicators were triggered");
        }

        AiModelClient.AiPredictionResponse aiPrediction = aiClient.predict(features).orElse(null);

        return new TelemetryRiskReport(
            SCORING_VERSION,
            riskScore,
            reviewLevel,
            evidenceConfidence,
            indicators,
            features,
            aiPrediction
        );
    }

    private int calcRiskScore(TelemetryFeatureService.TelemetryFeatures features) {
        int score = 0;
        double pasteFraction = features.pasteFraction();

        if (pasteFraction >= 0.8) {
            score += 35;
        } else if (pasteFraction >= 0.6) {
            score += 28;
        } else if (pasteFraction >= 0.4) {
            score += 20;
        } else if (pasteFraction >= 0.2) {
            score += 10;
        }

        long largePaste = features.largestPasteCharacters();
         if (largePaste >= 800) {
            score += 20;
        } else if (largePaste >= 400) {
            score += 16;
        } else if (largePaste >= 200) {
            score += 12;
        } else if (largePaste >= 100) {
            score += 8;
        }

        if (features.largePasteCount() >= 3) {
            score += 10;
        } else if (features.largePasteCount() >= 1) {
            score += 5;
        }

        if (features.largePasteAfterTabReturnCount() >= 2) {
            score += 20;
        } else if (features.largePasteAfterTabReturnCount() == 1) {
            score += 15;
        }

        if (features.pastedCharacters() >= 100) {
            if (features.reworkRatio() < 0.03) {
                score += 10;
            } else if (features.reworkRatio() < 0.1) {
                score += 6;
            } else if (features.reworkRatio() < 0.2) {
                score += 3;
            }
        }

        return Math.min(score, 100);
    }

    private int calcEvidenceConfidence(TelemetryFeatureService.TelemetryFeatures features) {
        int confidence = 0;
        confidence += Math.min(25, (int) (features.eventCount()));
        confidence += Math.min(20, (int) (features.activeDurationSeconds() / 15));

        long insertedCharacters = features.typedCharacters() + features.pastedCharacters();
        confidence += Math.min(25, (int) (insertedCharacters / 20));

        if (features.typingBatchCount() > 0) {
            confidence += 10;
        }

        if (features.pasteEvents() > 0 || features.deletionEdits() > 0) {
            confidence += 5;
        }

        if (features.runCount() > 0) {
            confidence += 5;
        }

        if (features.tabHiddenCount() > 0 || features.focusLostCount() > 0) {
            confidence += 5;
        }

        if (features.submitCount() > 0) {
            confidence += 5;
        }

        return Math.min(confidence, 100);
    }

    private String determineReviewLevel(int riskScore) {
        if (riskScore >= 70) {
            return "HIGH";
        }
        if (riskScore >= 40) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private List<String> buildIndicators(TelemetryFeatureService.TelemetryFeatures features) {
        List<String> indicators = new ArrayList<>();
        if (features.pasteFraction() >= 0.6) {
            double percentage = features.pasteFraction() * 100;
            indicators.add(String.format("%.1f%% of observed insertion was pasted", percentage));
        }

        if (features.largestPasteCharacters() >= 100) {
            indicators.add("Large paste detection: " + features.largestPasteCharacters() + " characters");
        }

        if (features.largePasteAfterTabReturnCount() > 0) {
            indicators.add(features.largePasteAfterTabReturnCount() + " large paste event(s) occurred shortly after returning to IDE");
        }

        if (features.pastedCharacters() >= 100 && features.reworkRatio() < 0.1) {
            double percentage = features.reworkRatio() * 100;
            indicators.add(String.format("Low rework observed relative to inserted code: %.1f%%", percentage));
        }

        return indicators;
    }

    public record TelemetryRiskReport(String scoringVersion, int riskScore, String reviewLevel, int evidenceConfidence, List<String> indicators, TelemetryFeatureService.TelemetryFeatures features, AiModelClient.AiPredictionResponse aiPrediction){}


}