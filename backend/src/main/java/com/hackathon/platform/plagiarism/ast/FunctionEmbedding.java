package com.hackathon.platform.plagiarism.ast;

/**
 * One function-level embedding vector from the AST service's /embed.
 * truncated is true if the source for this function exceeded the embedding model's token limit
 * and was cut off
 */
public record FunctionEmbedding(String qualifiedName, float[] vector, boolean truncated) {}
