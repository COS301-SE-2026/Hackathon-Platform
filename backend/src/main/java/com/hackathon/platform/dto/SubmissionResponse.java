package com.hackathon.platform.dto;

import java.math.BigDecimal;
import java.math.Instant;
import java.util.List;
import java.util.UUID;

public class SubmissionResponse {
    private Long submissionId;
    private UUID teamId;
    private Long levelId;
    private Long solverVersionId;
    private BigDecimal score;
    private String status;
    private Instant submittedAt;
    private String outputName;
    private String sourceName;
    private List<ScoringLogResponse> logs;

    public SubmissionResponse () {}

    public SubmissionResponse (
        Long submissionId,
        UUID teamId,
        Long levelId,
        Long solverVersionId,
        BigDecimal score,
        String status,
        Instant submittedAt,
        String outputName,
        String sourceName,
        List<ScoringLogResponse> logs) 
        {
            this.submissionId = submissionId;
            this.teamId = teamId;
            this.levelId = levelId;
            this.solverVersionId = solverVersionId;
            this.score = score;
            this.status = status;
            this.submittedAt = submittedAt;
            this.outputName = outputName;
            this.sourceName = sourceName;
            this.logs = logs;
        }
}