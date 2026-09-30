package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.platform.controller.AdminTelemetryController.AdminWorkspaceResponse;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.Level;
import com.hackathon.platform.repository.CodeWorkspaceRepository;
import com.hackathon.platform.repository.LevelRepository;
import com.hackathon.platform.service.TelemetryFeatureService.TelemetryFeatures;
import com.hackathon.platform.service.TelemetryRiskService;
import com.hackathon.platform.service.TelemetryRiskService.TelemetryRiskReport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminTelemetryControllerTest {
  @Mock private TelemetryRiskService riskService;
  @Mock private CodeWorkspaceRepository workspaceRepo;
  @Mock private LevelRepository levelRepo;
  @InjectMocks private AdminTelemetryController controller;

  private final UUID eventId = UUID.randomUUID();
  private final UUID teamId = UUID.randomUUID();

  @Test
  void getReport_returnsReportFromRiskService() {
    UUID workspaceId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    TelemetryRiskReport report =
        new TelemetryRiskReport(
            "BEHAVIOR_V1", 0, "INSUFFICIENT_DATA", 0, List.of(), TelemetryFeatures.empty(), null);
    when(riskService.analyze(workspaceId, userId)).thenReturn(report);

    assertThat(controller.getReport(workspaceId, userId)).isEqualTo(report);
  }

  @Test
  void getTeamWorkspaces_mapsWorkspacesWithLevelNumbers() {
    UUID workspaceId = UUID.randomUUID();
    CodeWorkspace workspace = mock(CodeWorkspace.class);
    when(workspace.getWorkspaceId()).thenReturn(workspaceId);
    when(workspace.getLevelId()).thenReturn((short) 2);
    when(workspaceRepo.findByEventIdAndTeamIdOrderByLevelIdAsc(eventId, teamId))
        .thenReturn(List.of(workspace));
    when(levelRepo.findById((short) 2))
        .thenReturn(Optional.of(new Level(UUID.randomUUID(), "Level", (short) 5)));

    List<AdminWorkspaceResponse> res = controller.getTeamWorkspaces(eventId, teamId);

    assertThat(res).containsExactly(new AdminWorkspaceResponse(workspaceId, (short) 2, (short) 5));
  }

  @Test
  void getTeamWorkspaces_throws_whenLevelMissing() {
    CodeWorkspace workspace = mock(CodeWorkspace.class);
    when(workspace.getLevelId()).thenReturn((short) 2);
    when(workspaceRepo.findByEventIdAndTeamIdOrderByLevelIdAsc(eventId, teamId))
        .thenReturn(List.of(workspace));
    when(levelRepo.findById((short) 2)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.getTeamWorkspaces(eventId, teamId))
        .isInstanceOf(IllegalStateException.class);
  }
}
