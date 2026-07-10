package com.hackathon.platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class LeaderboardEntryResponse {
    private final int rank;
    private final UUID teamId;
    private final String teamName;
    private final BigDecimal bestScore;
    private final Instant lastScoredAt;
}