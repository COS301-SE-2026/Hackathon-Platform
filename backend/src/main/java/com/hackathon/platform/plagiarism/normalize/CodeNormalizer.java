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
 * <p>This implementation is a lexer-based approximation rather than a real per-language AST parse
 * (no tree-sitter / javaparser dependency required). Used as fallback
 */
@Component
public class CodeNormalizer {

  private static final Set<String> COMMON_KEYWORDS =
      Set.of(
          // control flow / structure keywords shared across most C-like + Python languages
          "if",
          "else",
          "elif",
          "for",
          "while",
          "do",
          "switch",
          "case",
          "default",
          "break",
          "continue",
          "return",
          "try",
          "catch",
          "except",
          "finally",
          "throw",
          "throws",
          "raise",
          "class",
          "interface",
          "enum",
          "struct",
          "def",
          "function",
          "func",
          "fn",
          "public",
          "private",
          "protected",
          "static",
          "final",
          "const",
          "let",
          "var",
          "new",
          "import",
          "package",
          "from",
          "as",
          "extends",
          "implements",
          "with",
          "lambda",
          "async",
          "await",
          "yield",
          "in",
          "is",
          "not",
          "and",
          "or",
          "null",
          "none",
          "nil",
          "true",
          "false",
          "void",
          "int",
          "long",
          "double",
          "float",
          "boolean",
          "bool",
          "char",
          "string",
          "str",
          "self",
          "this",
          "super");

  /** Language family detected from file extension/shebang; */
  public enum Lang {
    C_LIKE, // java, c, cpp, cs, js, ts, go, rust, kotlin, swift ...
    PYTHON,
    UNKNOWN
  }

  private static final String STRING_ALT = "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'";
  private static final String ID_ALT = "[A-Za-z_][A-Za-z0-9_]*";
  private static final String NUM_ALT = "\\b\\d+(?:\\.\\d+)?[fFlLdD]?\\b";
  private static final String SYM_ALT =
      "==|!=|<=|>=|&&|\\|\\||\\+\\+|--|->|::|[{}()\\[\\];,.:+\\-*/%<>=!&|^~?]";

  // Group names must be [A-Za-z][A-Za-z0-9]* -- no underscores.
  private static final Pattern PYTHON_PATTERN =
      Pattern.compile(
          "(?<TRIPLE>\"\"\".*?\"\"\"|'''.*?''')"
              + "|(?<PYCOMMENT>#[^\\n]*)"
              + "|(?<STRLIT>"
              + STRING_ALT
              + ")"
              + "|(?<IDENT>"
              + ID_ALT
              + ")"
              + "|(?<NUMLIT>"
              + NUM_ALT
              + ")"
              + "|(?<SYMBOL>"
              + SYM_ALT
              + ")",
          Pattern.DOTALL);

  private static final Pattern C_LIKE_PATTERN =
      Pattern.compile(
          "(?<BLOCKCMT>/\\*.*?\\*/)"
              + "|(?<LINECMT>//[^\\n]*)"
              + "|(?<STRLIT>"
              + STRING_ALT
              + ")"
              + "|(?<IDENT>"
              + ID_ALT
              + ")"
              + "|(?<NUMLIT>"
              + NUM_ALT
              + ")"
              + "|(?<SYMBOL>"
              + SYM_ALT
              + ")",
          Pattern.DOTALL);

  private static final Pattern UNKNOWN_PATTERN =
      Pattern.compile(
          "(?<BLOCKCMT>/\\*.*?\\*/)"
              + "|(?<LINECMT>//[^\\n]*)"
              + "|(?<PYCOMMENT>#[^\\n]*)"
              + "|(?<STRLIT>"
              + STRING_ALT
              + ")"
              + "|(?<IDENT>"
              + ID_ALT
              + ")"
              + "|(?<NUMLIT>"
              + NUM_ALT
              + ")"
              + "|(?<SYMBOL>"
              + SYM_ALT
              + ")",
          Pattern.DOTALL);

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

  /**
   * Produces normalized structural token sequence for the given source.
   *
   * @param source raw file contents
   * @param lang language family
   * @return ordered list of normalized tokens
   */
  public List<String> normalize(String source, Lang lang) {
    List<NormalizedToken> tokens = normalizeWithOffsets(source, lang, "");
    List<String> texts = new ArrayList<>(tokens.size());
    for (NormalizedToken t : tokens) {
      texts.add(t.text());
    }
    return texts;
  }

  /**
   * Produces the normalized structural token sequence for the given source, with each token
   * carrying its real character offset.
   *
   * @param source raw file contents
   * @param lang language family
   * @param fileName logical file name to stamp onto every emitted token (purely metadata)
   * @return ordered list of normalized tokens, each with its source position
   */
  public List<NormalizedToken> normalizeWithOffsets(String source, Lang lang, String fileName) {
    if (source == null || source.isBlank()) {
      return List.of();
    }

    Pattern pattern =
        switch (lang) {
          case PYTHON -> PYTHON_PATTERN;
          case C_LIKE -> C_LIKE_PATTERN;
          default -> UNKNOWN_PATTERN;
        };

    List<NormalizedToken> tokens = new ArrayList<>();
    Matcher m = pattern.matcher(source);
    while (m.find()) {

      // comments contribute no token at all
      if (isSkipGroup(m, "TRIPLE")
          || isSkipGroup(m, "PYCOMMENT")
          || isSkipGroup(m, "BLOCKCMT")
          || isSkipGroup(m, "LINECMT")) {
        continue;
      }

      int start = m.start();
      int end = m.end();

      if (isSkipGroup(m, "STRLIT")) {
        tokens.add(new NormalizedToken(fileName, "STR", start, end));
        continue;
      }

      String idMatch = safeGroup(m, "IDENT");
      if (idMatch != null) {

        String lower = idMatch.toLowerCase(Locale.ROOT);
        String text = COMMON_KEYWORDS.contains(lower) ? lower : "ID";
        tokens.add(new NormalizedToken(fileName, text, start, end));
        continue;
      }

      if (safeGroup(m, "NUMLIT") != null) {
        tokens.add(new NormalizedToken(fileName, "NUM", start, end));
        continue;
      }

      String symMatch = safeGroup(m, "SYMBOL");
      if (symMatch != null) {
        tokens.add(new NormalizedToken(fileName, symMatch, start, end));
      }
    }
    return tokens;
  }

  private boolean isSkipGroup(Matcher m, String groupName) {
    return safeGroup(m, groupName) != null;
  }

  /**
   * throws if the named group doesn't exist in this pattern at all (rather than just not matching),
   * so guard every lookup
   */
  private String safeGroup(Matcher m, String groupName) {
    try {
      return m.group(groupName);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
