package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hackathon.platform.service.AiModelClient.AiPredictionResponse;
import com.hackathon.platform.service.TelemetryFeatureService.TelemetryFeatures;
import com.hackathon.platform.service.TelemetryRiskService.TelemetryRiskReport;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelemetryRiskServiceTest {
  @Mock private TelemetryFeatureService featService;
  @Mock private AiModelClient aiClient;
  private TelemetryRiskService service;

  private final UUID workspaceId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new TelemetryRiskService(featService, aiClient);
  }

  private static TelemetryFeatures features(
      long eventCount,
      long typedChars,
      long pastedChars,
      long pasteEvents,
      long largePasteCount,
      long largestPaste,
      long largePasteAfterReturn,
      double pasteFraction,
      double reworkRatio,
      long activeSeconds,
      long typingBatches) {
    return new TelemetryFeatures(
        eventCount,
        1,
        typedChars,
        0,
        typingBatches,
        0,
        pastedChars,
        pasteEvents,
        largePasteCount,
        largestPaste,
        0,
        pasteFraction,
        0,
        0,
        reworkRatio,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        largePasteAfterReturn,
        0,
        0,
        0,
        0,
        0,
        0,
        activeSeconds);
  }

  private TelemetryRiskReport analyze(TelemetryFeatures features) {
    when(featService.extractFeatures(workspaceId, userId)).thenReturn(features);
    return service.analyze(workspaceId, userId);
  }

  @Test
  void analyze_returnsInsufficientData_whenNoEvents() {
    TelemetryRiskReport report = analyze(TelemetryFeatures.empty());

    assertThat(report.reviewLevel()).isEqualTo("INSUFFICIENT_DATA");
    assertThat(report.riskScore()).isZero();
    assertThat(report.evidenceConfidence()).isZero();
    assertThat(report.aiPrediction()).isNull();
    verifyNoInteractions(aiClient);
  }

  @Test
  void analyze_isLowRisk_whenCodeWasMostlyTypes() {
    TelemetryFeatures typed = features(30, 1000, 0, 0, 0, 0, 0, 0, 0.1, 600, 5);
    when(aiClient.predict(typed)).thenReturn(Optional.empty());
    TelemetryRiskReport report = analyze(typed);

    assertThat(report.reviewLevel()).isEqualTo("LOW");
    assertThat(report.riskScore()).isZero();
    assertThat(report.evidenceConfidence()).isEqualTo(80);
    assertThat(report.indicators()).containsExactly("no indicators were triggered");
    assertThat(report.aiPrediction()).isNull();
    assertThat(report.scoringVersion()).isEqualTo("BEHAVIOR_V1");
  }

  @Test
  void analyze_isMediumRisk_forModeratePasting() {
    TelemetryFeatures feat = features(30, 200, 200, 2, 1, 450, 1, 0.5, 0.5, 300, 3);
    when(aiClient.predict(feat)).thenReturn(Optional.empty());
    TelemetryRiskReport report = analyze(feat);

    assertThat(report.riskScore()).isEqualTo(56);
    assertThat(report.reviewLevel()).isEqualTo("MEDIUM");
  }

  @Test
  void is_HighRiskWithIndicatorsAndAiPrediction_forHeavyPasting() {
    TelemetryFeatures feat = features(40, 50, 500, 3, 3, 900, 2, 0.9, 0.01, 600, 1);
    AiPredictionResponse prediction = new AiPredictionResponse("v1", 0.9, 90, 0.5, true);
    when(aiClient.predict(feat)).thenReturn(Optional.of(prediction));
    TelemetryRiskReport report = analyze(feat);

    assertThat(report.riskScore()).isEqualTo(95);
    assertThat(report.reviewLevel()).isEqualTo("HIGH");
    assertThat(report.indicators()).hasSize(4).contains("Large paste detection: 900 characters");
    assertThat(report.aiPrediction()).isEqualTo(prediction);
  }

  @Test
  void analyze_returnsInsufficientData_whenEvidenceIsTooThin() {
    TelemetryFeatures feat = features(3, 0, 100, 1, 1, 100, 0, 1.0, 0, 0, 0);
    when(aiClient.predict(feat)).thenReturn(Optional.empty());
    TelemetryRiskReport report = analyze(feat);

    assertThat(report.evidenceConfidence()).isLessThan(40);
    assertThat(report.reviewLevel()).isEqualTo("INSUFFICIENT_DATA");
  }
}
