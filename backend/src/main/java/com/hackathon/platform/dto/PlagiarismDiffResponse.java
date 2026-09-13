package com.hackathon.platform.dto;

import java.util.List;

/**
 * @param tokensA normalized structural tokens for submission A
 * @param tokensB same, for B
 * @param matchedRangesA [start, end) token index ranges in A that are part of a shared k-gram
 * @param matchedRangesB same, for B
 */
public record PlagiarismDiffResponse(
    Long submissionIdA,
    Long submissionIdB,
    List<String> tokensA,
    List<String> tokensB,
    List<int[]> matchedRangesA,
    List<int[]> matchedRangesB,
    double structuralScore) {}
