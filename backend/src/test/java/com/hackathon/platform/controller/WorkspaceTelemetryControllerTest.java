package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.ide.TelemetryBatchRequest;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.TelemetryFeatureService;
import com.hackathon.platform.service.TelemetryFeatureService.TelemetryFeatures;
import com.hackathon.platform.service.TelemetryRiskService;
import com.hackathon.platform.service.WorkspaceTelemetryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceTelemetryControllerTest {
  @Mock private WorkspaceTelemetryService telService;
  @Mock private TelemetryFeatureService featureService;
  @Mock private TelemetryRiskService riskService;
  @InjectMocks private WorkspaceTelemetryController controller;

  private final UUID workspaceId = UUID.randomUUID();
  private final User user = User.builder().userId(UUID.randomUUID()).build();

  @Test
  void startSession_returnsSessionId() {
    UUID sessionId = UUID.randomUUID();
    when(telService.startSession(workspaceId, user)).thenReturn(sessionId);

    assertThat(controller.startSession(workspaceId, user)).containsEntry("sessionId", sessionId);
  }

  @Test
  void saveBatch_delegatesToService() {
    TelemetryBatchRequest req = new TelemetryBatchRequest(UUID.randomUUID(), List.of());

    controller.saveBatch(workspaceId, req, user);

    verify(telService).saveBatch(workspaceId, req, user);
  }

  @Test
  void endSession_delegatesToService() {
    UUID sessionId = UUID.randomUUID();

    controller.endSession(workspaceId, sessionId, user);

    verify(telService).endSession(workspaceId, sessionId, user);
  }

  @Test
  void getFeatures_usesCurrentUsersId() {
    when(featureService.extractFeatures(workspaceId, user.getUserId()))
        .thenReturn(TelemetryFeatures.empty());

    assertThat(controller.getFeatures(workspaceId, user)).isEqualTo(TelemetryFeatures.empty());
  }
}
