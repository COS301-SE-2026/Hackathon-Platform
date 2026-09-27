package com.hackathon.platform.plagiarism.ast;

/**
 * One normalized token returned by the AST parsing microservice, together with its original source
 * byte offsets (end-exclusive). Mirrors NormalizedToken in shape and purpose
 */
public record AstToken(String text, int start, int end) {}
