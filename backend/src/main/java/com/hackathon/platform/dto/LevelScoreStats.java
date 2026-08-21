package com.hackathon.platform.dto;

import java.math.BigDecimal;

public class LevelScoreStats {

    private short levelId;
    private String levelName;
    private long scoredSubmissions;

    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal avgScore;

    public LevelScoreStats() {}

    public LevelScoreStats(
        short levelId,
        String levelName,
        long scoredSubmissions,
        BigDecimal minScore,
        BigDecimal maxScore,
        BigDecimal avgScore
    ) {
        this.levelId = levelId;
        this.levelName = levelName;
        this.scoredSubmissions = scoredSubmissions;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.avgScore = avgScore;

    }

    public short getLevelId() {
        return levelId;
    }

    public void setLevelId(short levelId) {
        this.levelId = levelId;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public long getScoredSubmissions() {
        return scoredSubmissions;
    }

    public void setScoredSubmissions(long scoredSubmissions) {
        this.scoredSubmissions = scoredSubmissions;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public void setMinScore(BigDecimal minScore) {
        this.minScore = minScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public BigDecimal getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(BigDecimal avgScore) {
        this.avgScore = avgScore;
    }

}