package com.hackathon.platform.plagiarism.normalize;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Turns raw source code into a normalized structural token sequence.
 *
 * This implementation is a lexer-based approximation rather than a real per-language
 * AST parse (no tree-sitter / javaparser dependency required).
 */
@Component
public class CodeNormalizer {

    private static final Set<String> COMMON_KEYWORDS =
      Set.of(
          // control flow / structure keywords shared across most C-like and Python languages
          "if", "else", "elif", "for", "while", "do", "switch", "case", "default", "break",
          "continue", "return", "try", "catch", "except", "finally", "throw", "throws", "raise",
          "class", "interface", "enum", "struct", "def", "function", "func", "fn", "public",
          "private", "protected", "static", "final", "const", "let", "var", "new", "import",
          "package", "from", "as", "extends", "implements", "with", "lambda", "async", "await",
          "yield", "in", "is", "not", "and", "or", "null", "none", "nil", "true", "false", "void",
          "int", "long", "double", "float", "boolean", "bool", "char", "string", "str", "self",
          "this", "super");

  /** Language family detected from file extension/shebang;*/
  public enum Lang {

    C_LIKE, // java, c, cpp, cs, js, ts, go, rust, kotlin, swift ...
    PYTHON,
    UNKNOWN
  }

  private static final Pattern C_LINE_COMMENT = Pattern.compile("//.*");
  private static final Pattern C_BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
  private static final Pattern PY_COMMENT = Pattern.compile("#.*");
  private static final Pattern PY_TRIPLE_STRING =
      Pattern.compile("(\"\"\".*?\"\"\"|'''.*?''')", Pattern.DOTALL);
  private static final Pattern STRING_LITERAL =
      Pattern.compile("\"(\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])*'");
  private static final Pattern NUMBER_LITERAL = Pattern.compile("\\b\\d+(\\.\\d+)?[fFlLdD]?\\b");
  private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");
  private static final String STRING_SENTINEL = "STRTOKEN";
  private static final Pattern SYMBOL =
      Pattern.compile("(==|!=|<=|>=|&&|\\|\\||\\+\\+|--|->|::|[{}()\\[\\];,.:+\\-*/%<>=!&|^~?])");

  public Lang detectLanguage(String fileName) {
    if (fileName == null) {
      return Lang.UNKNOWN;

    }
    String lower = fileName.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".py")) {
      return Lang.PYTHON;

    }
    if (lower.endsWith(".java")
        || lower.endsWith(".c")
        || lower.endsWith(".h")
        || lower.endsWith(".cpp")
        || lower.endsWith(".hpp")
        || lower.endsWith(".cs")
        || lower.endsWith(".js")
        || lower.endsWith(".ts")
        || lower.endsWith(".jsx")
        || lower.endsWith(".tsx")
        || lower.endsWith(".go")
        || lower.endsWith(".rs")
        || lower.endsWith(".kt")
        || lower.endsWith(".swift")) {
      return Lang.C_LIKE;

    }

    return Lang.UNKNOWN;
  }

}
