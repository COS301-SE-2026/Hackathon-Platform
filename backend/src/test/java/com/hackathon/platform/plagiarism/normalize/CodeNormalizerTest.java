package com.hackathon.platform.plagiarism.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.platform.plagiarism.normalize.CodeNormalizer.Lang;
import java.util.List;
import org.junit.jupiter.api.Test;

class CodeNormalizerTest {

  private final CodeNormalizer normalizer = new CodeNormalizer();

  @Test
  void detectLanguage_nullFileName_returnsUnknown() {

    assertThat(normalizer.detectLanguage(null)).isEqualTo(Lang.UNKNOWN);
  }

  @Test
  void detectLanguage_pyExtension_returnsPython() {

    assertThat(normalizer.detectLanguage("main.py")).isEqualTo(Lang.PYTHON);
    assertThat(normalizer.detectLanguage("Main.PY")).isEqualTo(Lang.PYTHON);
  }

  @Test
  void detectLanguage_cLikeExtensions_returnCLike() {

    List<String> cLikeNames =
        List.of(
            "Main.java",
            "a.c",
            "a.h",
            "a.cpp",
            "a.hpp",
            "a.cs",
            "a.js",
            "a.ts",
            "a.jsx",
            "a.tsx",
            "a.go",
            "a.rs",
            "a.kt",
            "a.swift");

    for (String name : cLikeNames) {
      assertThat(normalizer.detectLanguage(name))
          .as("extension of %s", name)
          .isEqualTo(Lang.C_LIKE);
    }
  }

  @Test
  void detectLanguage_unrecognizedExtension_returnsUnknown() {

    assertThat(normalizer.detectLanguage("data.txt")).isEqualTo(Lang.UNKNOWN);
    assertThat(normalizer.detectLanguage("noextension")).isEqualTo(Lang.UNKNOWN);
  }

  @Test
  void normalizeWithOffsets_nullSource_returnsEmptyList() {

    assertThat(normalizer.normalizeWithOffsets(null, Lang.C_LIKE, "f.java")).isEmpty();
  }

  @Test
  void normalizeWithOffsets_blankSource_returnsEmptyList() {

    assertThat(normalizer.normalizeWithOffsets("  \n\t", Lang.C_LIKE, "f.java")).isEmpty();
  }

  @Test
  void normalizeWithOffsets_cLike_blockAndLineCommentsProduceNoTokens() {

    String source = "/* block\ncomment */ int /* mid */ x; // trailing\n";

    List<NormalizedToken> tokens = normalizer.normalizeWithOffsets(source, Lang.C_LIKE, "f.java");

    assertThat(tokens).extracting(NormalizedToken::text).containsExactly("int", "ID", ";");
  }

  @Test
  void normalizeWithOffsets_cLike_stringLiteralBecomesStrToken() {

    String source = "x = \"hello, world\";";

    List<NormalizedToken> tokens = normalizer.normalizeWithOffsets(source, Lang.C_LIKE, "f.java");

    assertThat(tokens).extracting(NormalizedToken::text).containsExactly("ID", "=", "STR", ";");
  }

  @Test
  void normalizeWithOffsets_cLike_singleQuotesStringAlsoBecomesStrToken() {

    List<NormalizedToken> tokens =
        normalizer.normalizeWithOffsets("char c = 'x';", Lang.C_LIKE, "f.java");

    assertThat(tokens)
        .extracting(NormalizedToken::text)
        .containsExactly("char", "ID", "=", "STR", ";");
  }

  @Test
  void normalizeWithOffsets_cLike_keywordsAreLowercasedAndPreserved() {

    List<NormalizedToken> tokens =
        normalizer.normalizeWithOffsets("PUBLIC static Void run", Lang.C_LIKE, "f.java");

    assertThat(tokens)
        .extracting(NormalizedToken::text)
        .containsExactly("public", "static", "void", "ID");
  }

  @Test
  void normalizeWithOffsets_cLike_unknownIdentifierBecomesIdToken() {

    List<NormalizedToken> tokens =
        normalizer.normalizeWithOffsets("myVariableName", Lang.C_LIKE, "f.java");

    assertThat(tokens).extracting(NormalizedToken::text).containsExactly("ID");
  }

  @Test
  void normalizeWithOffsets_cLike_numericLiteralBecomeNumToken() {

    List<NormalizedToken> tokens =
        normalizer.normalizeWithOffsets("int x = 42; double y = 3.14f;", Lang.C_LIKE, "f.java");

    assertThat(tokens)
        .extracting(NormalizedToken::text)
        .containsExactly("int", "ID", "=", "NUM", ";", "double", "ID", "=", "NUM", ";");
  }

  @Test
  void normalizeWithOffsets_cLike_multiCharSymbolsAreMatchedAsSingleTokens() {

    List<NormalizedToken> tokens =
        normalizer.normalizeWithOffsets("a == b && c->d", Lang.C_LIKE, "f.java");

    assertThat(tokens)
        .extracting(NormalizedToken::text)
        .containsExactly("ID", "==", "ID", "&&", "ID", "->", "ID");
  }

  @Test
  void normalizeWithOffsets_cLike_tokenOffsetsMatchOriginalSource() {

    String source = "int x;";

    List<NormalizedToken> tokens = normalizer.normalizeWithOffsets(source, Lang.C_LIKE, "f.java");

    assertThat(tokens).hasSize(3);
    assertThat(tokens.get(0).start()).isEqualTo(0);
    assertThat(tokens.get(0).end()).isEqualTo(3);
    assertThat(tokens.get(1).start()).isEqualTo(4);
    assertThat(tokens.get(1).end()).isEqualTo(5);
    assertThat(tokens.get(2).start()).isEqualTo(5);
    assertThat(tokens.get(2).end()).isEqualTo(6);
  }

  @Test
  void normalizeWithOffsets_cLike_fileNameStampedOntoEveryToken() {

    List<NormalizedToken> tokens =
        normalizer.normalizeWithOffsets("int x;", Lang.C_LIKE, "Main.java");

    assertThat(tokens).extracting(NormalizedToken::fileName).containsOnly("Main.java");
  }

  @Test
  void normalizeWithOffsets_python_tripleQuotedStringProducesNoToken() {

    String source = "\"\"\"docstring\nspanning lines\"\"\"\nx = 1";

    List<NormalizedToken> tokens = normalizer.normalizeWithOffsets(source, Lang.PYTHON, "f.py");

    assertThat(tokens).extracting(NormalizedToken::text).containsExactly("ID", "=", "NUM");
  }

  @Test
  void normalizeWithOffsets_python_tripleSingleQuoteAlsoProducesNoToken() {

    List<NormalizedToken> tokens =
        normalizer.normalizeWithOffsets("'''doc'''\ny = 2", Lang.PYTHON, "f.py");

    assertThat(tokens).extracting(NormalizedToken::text).containsExactly("ID", "=", "NUM");
  }

  @Test
  void normalizeWithOffsets_python_hashCommentProducesNoToken() {

    String source = "x = 1 # this is a comment\ny = 2";

    List<NormalizedToken> tokens = normalizer.normalizeWithOffsets(source, Lang.PYTHON, "f.py");

    assertThat(tokens)
        .extracting(NormalizedToken::text)
        .containsExactly("ID", "=", "NUM", "ID", "=", "NUM");
  }

  @Test
  void normalizeWithOffsets_python_stringLiteralBecomesStrToken() {

    List<NormalizedToken> tokens = normalizer.normalizeWithOffsets("x = 'hi'", Lang.PYTHON, "f.py");

    assertThat(tokens).extracting(NormalizedToken::text).containsExactly("ID", "=", "STR");
  }

  @Test
  void normalizeWithOffsets_python_keywordsRecognized() {

    List<NormalizedToken> tokens =
        normalizer.normalizeWithOffsets("def run(self):", Lang.PYTHON, "f.py");

    assertThat(tokens)
        .extracting(NormalizedToken::text)
        .containsExactly("def", "ID", "(", "self", ")", ":");
  }

  @Test
  void normalizeWithOffsets_unknownLang_recognizesBothCommentStyles() {

    String source = "x = 1 // c-style\ny = 2 # python-style\n/* block */ z = 3";

    List<NormalizedToken> tokens = normalizer.normalizeWithOffsets(source, Lang.UNKNOWN, "f.txt");

    assertThat(tokens)
        .extracting(NormalizedToken::text)
        .containsExactly("ID", "=", "NUM", "ID", "=", "NUM", "ID", "=", "NUM");
  }

  @Test
  void normalizeWithOffsets_unknownLang_classifiesLiteralAndSymbolsLikeCLike() {

    List<NormalizedToken> tokens =
        normalizer.normalizeWithOffsets("total += 1;", Lang.UNKNOWN, "f.txt");

    assertThat(tokens)
        .extracting(NormalizedToken::text)
        .containsExactly("ID", "+", "=", "NUM", ";");
  }

  @Test
  void normalize_returnsJustTokenTextsInOrder() {

    List<String> texts = normalizer.normalize("int x = 1;", Lang.C_LIKE);

    assertThat(texts).containsExactly("int", "ID", "=", "NUM", ";");
  }

  @Test
  void normalize_blankSource_returnsEmptyList() {
    assertThat(normalizer.normalize("", Lang.C_LIKE)).isEmpty();
  }
}
