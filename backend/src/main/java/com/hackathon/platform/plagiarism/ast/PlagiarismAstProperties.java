package com.hackathon.platform.plagiarism.ast;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Config for talking to the Python tree-sitter/CodeBERT sidecar. */
@Component
@ConfigurationProperties(prefix = "plagiarism.ast-service")
@Data
public class PlagiarismAstProperties {

  /** Master switch for the AST parsing path. false = always use the regex layer */
  private boolean enabled = true;

  /** Master switch for the embedding path. false = structural score only, no embedding score. */
  private boolean embeddingEnabled = true;

  private String baseUrl = "http://localhost:8008";

  private int connectTimeoutMs = 2000;

  /** Per-file /parse call timeout. */
  private int requestTimeoutMs = 5000;

  /**
   * Per-file /embed call timeout. Longer because passing through CodeBERT for every funcion in a
   * file.
   */
  private int embedRequestTimeoutMs = 20000;
}
