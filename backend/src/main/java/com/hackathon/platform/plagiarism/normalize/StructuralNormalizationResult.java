package com.hackathon.platform.plagiarism.normalize;

import com.hackathon.platform.plagiarism.ast.AstFunctionSpan;
import java.util.List;

/**
 * Result of normalizing one source file: the token stream feeding the structural (winnowing)
 * signal, plus whatever function spans were discovered for the embedding signal.
 */
public record StructuralNormalizationResult(
    List<NormalizedToken> tokens, List<AstFunctionSpan> functions, String source) {
        

  /** source is "ast" or "lexer" - purely for logging/metrics, not used in scoring. */
  public static StructuralNormalizationResult ast(List<NormalizedToken> tokens, List<AstFunctionSpan> functions) {
    return new StructuralNormalizationResult(tokens, functions, "ast");
  }

  public static StructuralNormalizationResult lexer(List<NormalizedToken> tokens) {
    return new StructuralNormalizationResult(tokens, List.of(), "lexer");

  }

}

