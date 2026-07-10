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

    public LeaderboardEntryResponse(int rank, UUID teamId, String teamName, BigDecimal bestScore, Instant lastScoredAt) {
        this.rank = rank;
        this.teamId = teamId;
        this.teamName = teamName;
        this.bestScore = bestScore;
        this.lastScoredAt = lastScoredAt;
    }

    public int getRank() {
        return rank;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public BigDecimal getBestScore() {
        return bestScore;
    }

    public Instant getLastScoredAt() {
        return lastScoredAt;
    }

}