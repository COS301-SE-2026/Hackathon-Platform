package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.platform.service.TelemetryFeatureService.TelemetryFeatures;
import org.junit.jupiter.api.Test;

class AiModelClientTest {
  @Test
  void predict_returnsEmpty_whenModelServiceIsUnreachable() {
    AiModelClient client = new AiModelClient("http://localhost:1");
    assertThat(client.predict(TelemetryFeatures.empty())).isEmpty();
  }
}
