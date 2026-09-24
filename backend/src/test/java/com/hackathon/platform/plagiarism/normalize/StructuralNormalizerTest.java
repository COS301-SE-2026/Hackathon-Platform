package com.hackathon.platform.plagiarism.normalize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.plagiarism.ast.AstFunctionSpan;
import com.hackathon.platform.plagiarism.ast.AstParseResponse;
import com.hackathon.platform.plagiarism.ast.AstServiceClient;
import com.hackathon.platform.plagiarism.ast.AstToken;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StructuralNormalizerTest {

  @Mock private AstServiceClient astClient;
  @Mock private CodeNormalizer lexerNormalizer;

  private StructuralNormalizer normalizer;

  @Test
  void normalize_astServiceOk_returnsAstTokensAndFunctionsWithoutTouchingLexer() {

    normalizer = new StructuralNormalizer(astClient, lexerNormalizer);

    List<AstToken> tokens = List.of(new AstToken("ID", 0, 2), new AstToken("=", 3, 4));
    List<AstFunctionSpan> functions = List.of(new AstFunctionSpan("Main.run", "function_definition", 0, 10, 1, 2));
    when(astClient.parse("Main.java", "content")).thenReturn(
        new AstParseResponse("ok", "java", tokens, functions, null)
    );

    StructuralNormalizationResult result = normalizer.normalize("Main.java", "content");

    assertThat(result.source()).isEqualTo("ast");
    assertThat(result.functions()).isEqualTo(functions);
    assertThat(result.tokens()).hasSize(2);
    assertThat(result.tokens().get(0).text()).isEqualTo("ID");
    assertThat(result.tokens().get(0).start()).isEqualTo(0);
    assertThat(result.tokens().get(0).fileName()).isEqualTo("Main.java");

    verify(lexerNormalizer, never()).normalizeWithOffsets(any(), any(), any());

  }

  @Test
  void normalize_astServiceUnsupportedLanguage_fallsBackToLexer() {

    normalizer = new StructuralNormalizer(astClient, lexerNormalizer);

    when(astClient.parse("script.rb", "content")).thenReturn(
        new AstParseResponse("unsupported_language", null, List.of(), List.of(), "no ruby grammar")
    );
    when(lexerNormalizer.detectLanguage("script.rb")).thenReturn(CodeNormalizer.Lang.UNKNOWN);
    when(lexerNormalizer.normalizeWithOffsets(eq("content"), eq(CodeNormalizer.Lang.UNKNOWN), eq("script.rb")))
        .thenReturn(List.of(new NormalizedToken("script.rb", "tok", 0, 3)));
    

    StructuralNormalizationResult result = normalizer.normalize("script.rb", "content");

    assertThat(result.source()).isEqualTo("lexer");
    assertThat(result.functions()).isEmpty();
    assertThat(result.tokens()).hasSize(1);
    

  }

  @Test
  void normalize_astServiceUnavailable_fallsBackToLexer() {

    normalizer = new StructuralNormalizer(astClient, lexerNormalizer);

    when(astClient.parse("Main.java", "content")).thenReturn(
        new AstParseResponse("service_unavailable", null, List.of(), List.of(), "connection refused")
    );
    when(lexerNormalizer.detectLanguage("Main.java")).thenReturn(CodeNormalizer.Lang.C_LIKE);
    when(lexerNormalizer.normalizeWithOffsets(eq("content"), eq(CodeNormalizer.Lang.C_LIKE), eq("Main.java")))
        .thenReturn(List.of());
    
    
    StructuralNormalizationResult result = normalizer.normalize("Main.java", "content");

    assertThat(result.source()).isEqualTo("lexer");
    assertThat(result.functions()).isEmpty();
    

  }

  @Test
  void normalize_astServiceParseError_fallsBackToLexer() {

    normalizer = new StructuralNormalizer(astClient, lexerNormalizer);

    when(astClient.parse("Broken.java", "not valid {{{")).thenReturn(
        new AstParseResponse("parse_error", "java", List.of(), List.of(), "unexpected token")
    );
    when(lexerNormalizer.detectLanguage("Broken.java")).thenReturn(CodeNormalizer.Lang.C_LIKE);
    when(lexerNormalizer.normalizeWithOffsets(any(), any(), any()))
        .thenReturn(List.of());
    
    
    StructuralNormalizationResult result = normalizer.normalize("Broken.java", "not valid {{{");

    assertThat(result.source()).isEqualTo("lexer");
    verify(lexerNormalizer).detectLanguage("Broken.java");
    

  }
}