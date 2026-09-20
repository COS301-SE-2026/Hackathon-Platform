package com.hackathon.platform.plagiarism.ast;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A function/method discovered while parsing a source file, with its byte offsets into the
 * original (not normalized) source. Consumed by the embedding stage to slice out real source text
 * per function for CodeBERT
 */
public record AstFunctionSpan(
    @JsonProperty("qualified_name") String qualifiedName,
    @JsonProperty("node_type") String nodeType,
    @JsonProperty("start_byte") int startByte,
    @JsonProperty("end_byte") int endByte,
    @JsonProperty("start_line") int startLine,
    @JsonProperty("end_line") int endLine) {}