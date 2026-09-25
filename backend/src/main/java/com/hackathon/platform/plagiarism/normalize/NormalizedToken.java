package com.hackathon.platform.plagiarism.normalize;

/**
 * One normalized structural token, together with where it came from in the original source.
 *
 * @param fileName the source file this token came from (a submission may span several files)
 * @param text the normalized token text
 * @param start character offset (inclusive) into the original file content
 * @param end character offset (exclusive) into the original file content
 */
public record NormalizedToken(String fileName, String text, int start, int end) {}
