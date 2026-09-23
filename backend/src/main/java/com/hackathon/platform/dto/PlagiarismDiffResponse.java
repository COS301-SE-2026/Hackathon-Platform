package com.hackathon.platform.dto;

import java.util.List;

/**
 * Side-by-side diff data for one flagged pair, over real source code and real character offsets.
 *
 * @param filesA every source file in submission A, as originally written
 * @param filesB same, for B
 * @param matchedRangesA character ranges in A's files that are part of a shared k-gram with B
 * @param matchedRangesB same, for B
 * @param structuralScore Jaccard similarity between the two submissions' fingerprint sets
 * @param functionMatches function-level semantic (embedding) matches between A and B, sorted by
 *     descending similarity.
 * @param semanticStatus why functionMatches looks the way it does -- see {@link SemanticStatus}.
 */
public record PlagiarismDiffResponse(
    Long submissionIdA,
    Long submissionIdB,
    List<SourceFileResponse> filesA,
    List<SourceFileResponse> filesB,
    List<MatchedRangeResponse> matchedRangesA,
    List<MatchedRangeResponse> matchedRangesB,
    double structuralScore,
    List<FunctionMatchResponse> functionMatches,
    SemanticStatus semanticStatus) {

    /**
     * Distinguishes "we never had a semantic signal for this pair" from "we had one and it just
     * didn't find anything worth flagging" 
     */
    public enum SemanticStatus {
        /** Both submissions have stored function embeddings, but no pair cleared the threshold. */
        NO_MATCHES_ABOVE_THRESHOLD,
        /** A has no stored function embeddings (every file fell back to the lexer, or embedding
         * was unavailable when it was last processed). */
        NO_DATA_FOR_A,
        /** Same as NO_DATA_FOR_A, but for B. */
        NO_DATA_FOR_B,
        /** Neither submission has stored function embeddings. */
        NO_DATA_FOR_EITHER,
        /** At least one function pair cleared the threshold*/
        MATCHED,
    }
}
