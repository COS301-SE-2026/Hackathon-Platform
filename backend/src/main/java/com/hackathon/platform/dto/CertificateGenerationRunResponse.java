package com.hackathon.platform.dto;

import com.hackathon.platform.model.CertificateGenerationRun;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificateGenerationRunResponse {
  private final UUID runId;
  private final UUID eventId;
  private final UUID templateId;
  private final String scope;
  private final Integer topN;
  private final String status;
  private final int totalCount;
  private final String errorMessage;
  private final OffsetDateTime requestedAt;
  private final OffsetDateTime completedAt;

  public CertificateGenerationRunResponse(CertificateGenerationRun run) {
    this.runId = run.getRunId();
    this.eventId = run.getEventId();
    this.templateId = run.getTemplateId();
    this.scope = run.getScope();
    this.topN = run.getTopN();
    this.status = run.getStatus();
    this.totalCount = run.getTotalCount();
    this.errorMessage = run.getErrorMessage();
    this.requestedAt = run.getRequestedAt();
    this.completedAt = run.getCompletedAt();
  }
}
