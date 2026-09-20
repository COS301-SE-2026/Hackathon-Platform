package com.hackathon.platform.dto;

/**
 * One function-level semantic match surfaced in the diff view, alongside the existing
 * k-gram/structural. This is the CodeBERT embedding signal made
 * visible per-function rather than as a single aggregate score : Structural score not enough alomne.
 *
 * code startA endA and startB endB are byte offsets into the
 * original source of fileNameA/fileNameB respectively, so the frontend can highlight or
 * scroll to the matched function the same way it already does for structural matches.
 *
 * @param similarity cosine similarity (0-1) after corpus centering.
 */
public record FunctionMatchResponse(
    
    String fileNameA,
    String functionNameA,
    int startA,
    int endA,
    String fileNameB,
    String functionNameB,
    int startB,
    int endB,
    double similarity

) {}
