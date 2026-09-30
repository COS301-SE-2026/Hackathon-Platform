package com.hackathon.platform.dto;

/**
 * @param levelId if null, runs across every level of the event
 * @param topN how many top-leaderboard teams (per level) to compare;
 */
public record PlagiarismRunRequest(Short levelId, Integer topN) {}
