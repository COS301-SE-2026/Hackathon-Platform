package com.hackathon.platform.plagiarism.normalize;

import com.hackathon.platform.plagiarism.ast.AstParseResponse;
import com.hackathon.platform.plagiarism.ast.AstServiceClient;
import com.hackathon.platform.plagiarism.ast.AstToken;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Single entry point for turning one source file into a normalized structural token stream. Tries
 * the tree-sitter AST service first; falls back to the regex-lexer CodeNormalizer whenever the AST
 * service reports the language is unsupported, fails to parse, or is unreachable.
 */
@Component
@RequiredArgsConstructor
public class StructuralNormalizer {

  private static final Logger logger = LoggerFactory.getLogger(StructuralNormalizer.class);

  private final AstServiceClient astClient;
  private final CodeNormalizer lexerNormalizer;

  public StructuralNormalizationResult normalize(String fileName, String content) {

    AstParseResponse response = astClient.parse(fileName, content);

    if (response.isOk()) {

      List<NormalizedToken> tokens =
          response.tokens().stream().map(t -> toNormalizedToken(fileName, t)).toList();
      return StructuralNormalizationResult.ast(tokens, response.functions());
    }

    // "unsupported_language" / "parse_error" / "service_unavailable" all take the same fallback
    // path
    logger.debug(
        "Falling back to regex lexer for {} (ast status={}, detail={})",
        fileName,
        response.status(),
        response.detail());

    CodeNormalizer.Lang lang = lexerNormalizer.detectLanguage(fileName);
    List<NormalizedToken> tokens = lexerNormalizer.normalizeWithOffsets(content, lang, fileName);
    return StructuralNormalizationResult.lexer(tokens);
  }

  private NormalizedToken toNormalizedToken(String fileName, AstToken token) {

    return new NormalizedToken(fileName, token.text(), token.start(), token.end());
  }
}
