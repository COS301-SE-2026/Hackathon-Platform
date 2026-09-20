package com.hackathon.platform.dto;

/**
 * A single matched structural fragment, as a character range (end-exclusive) into the original
 * source of file.
 *  This is what lets the frontend highlight real code the participant wrote.
 */
public record MatchedRangeResponse(String fileName, int start, int end) {}
