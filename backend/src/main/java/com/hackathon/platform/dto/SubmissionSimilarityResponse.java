package com.hackathon.platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubmissionSimilarityResponse(
    Long id,
    short levelId,
    Long submissionIdA,
    Long submissionIdB,
    UUID teamIdA,
    String teamNameA,
    UUID teamIdB,
    String teamNameB,
    BigDecimal structuralScore,
    BigDecimal embeddingScore,
    BigDecimal combinedScore,
    int matchedKgramCount,
    boolean flagged,
    Instant runAt) {}
