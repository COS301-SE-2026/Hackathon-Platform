package com.hackathon.platform.dto;

import com.hackathon.platform.dto.ScoringLogResponse;
import java.math.BigDecimal;
import java.time.Instant;
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

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }
    
    public Long getSubmissionId() {
        return submissionId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setLevelId(Long levelId) {
        this.levelId = levelId;
    }

    public Long getLevelId() {
        return levelId;
    }

    public void setSolverVersionId(Long solverVersionId) {
        this.solverVersionId = solverVersionId;
    }

    public Long getSolverVersionId() {
        return solverVersionId;
    }

    public void setScore (BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getOutputName() {
        return outputName;
    }

    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName){
        this.sourceName = sourceName;
    }

    public List<ScoringLogResponse> getLogs() {
        return logs;
    }

    public void setLogs(List<ScoringLogResponse> logs) {
        this.logs = logs;
    }
}