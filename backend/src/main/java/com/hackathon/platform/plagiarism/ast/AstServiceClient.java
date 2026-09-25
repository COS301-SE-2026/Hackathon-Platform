package com.hackathon.platform.plagiarism.ast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Thin HTTP client for the Python tree-sitter/CodeBERT microservice. */
@Component
public class AstServiceClient {

  private static final Logger logger = LoggerFactory.getLogger(AstServiceClient.class);

  private final HttpClient httpClient;
  private final ObjectMapper mapper;
  private final PlagiarismAstProperties props;

  public AstServiceClient(PlagiarismAstProperties props) {

    this.props = props;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))

            // Pin to HTTP/1.1 explicitly due to upgrade preface corrupting framing on reused
            // connection.
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    this.mapper =
        new ObjectMapper()
            // Kept as a defense fallback, do not rely on alone.

            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private record ParseRequestBody(
      @JsonProperty("file_name") String fileName, @JsonProperty("content") String content) {}

  /**
   * Calls /parse. Returns a response with status "service_unavailable" (an internal-only status,
   * never sent by the Python side) on any network/timeout/deserialization failure or non-2xx status
   * code. Callers should treat this identically to "unsupported_language" / "parse_error", i.e.
   * fall back to the regex lexer.
   */
  public AstParseResponse parse(String fileName, String content) {

    if (!props.isEnabled()) {
      return new AstParseResponse(
          "service_unavailable", null, List.of(), List.of(), "ast service disabled");
    }

    try {
      String body = mapper.writeValueAsString(new ParseRequestBody(fileName, content));
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(props.getBaseUrl() + "/parse"))
              .timeout(Duration.ofMillis(props.getRequestTimeoutMs()))
              .header("Content-Type", "application/json")
              .POST(BodyPublishers.ofString(body))
              .build();

      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        logger.warn(
            "AST service /parse returned HTTP {} for {}, falling back to lexer. Sent: {} | Response body: {}",
            response.statusCode(),
            fileName,
            truncateForLog(body),
            truncateForLog(response.body()));

        return new AstParseResponse(
            "service_unavailable", null, List.of(), List.of(), "http " + response.statusCode());
      }
      return mapper.readValue(response.body(), AstParseResponse.class);

    } catch (IOException | InterruptedException | RuntimeException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      logger.warn(
          "AST service /parse call failed for {}, falling back to lexer: {}",
          fileName,
          e.getMessage());
      return new AstParseResponse(
          "service_unavailable", null, List.of(), List.of(), e.getMessage());
    }
  }

  private String truncateForLog(String s) {
    if (s == null) {
      return "null";
    }
    return s.length() <= 500 ? s : s.substring(0, 500) + "...(truncated)";
  }

  private record SpanRequestBody(
      @JsonProperty("qualified_name") String qualifiedName,
      @JsonProperty("start_byte") int startByte,
      @JsonProperty("end_byte") int endByte) {}

  private record EmbedRequestBody(
      @JsonProperty("file_name") String fileName,
      @JsonProperty("content") String content,
      @JsonProperty("spans") List<SpanRequestBody> spans) {}

  private record EmbeddingWire(
      @JsonProperty("qualified_name") String qualifiedName,
      @JsonProperty("vector") float[] vector,
      @JsonProperty("truncated") boolean truncated) {}

  private record EmbedResponseWire(
      @JsonProperty("status") String status,
      @JsonProperty("model") String model,
      @JsonProperty("dimension") Integer dimension,
      @JsonProperty("embeddings") List<EmbeddingWire> embeddings,
      @JsonProperty("detail") String detail) {}

  /**
   * Calls /embed for a batch of function spans belonging to one file's source. Returns an empty
   * list if embedding backend unavailable. Run doesnt fail in that case, just based on strucutrual
   * score alone.
   */
  public List<FunctionEmbedding> embed(
      String fileName, String content, List<AstFunctionSpan> spans) {
    if (!props.isEmbeddingEnabled() || spans.isEmpty()) {
      return List.of();
    }
    try {
      List<SpanRequestBody> spanBodies =
          spans.stream()
              .map(s -> new SpanRequestBody(s.qualifiedName(), s.startByte(), s.endByte()))
              .toList();

      String body = mapper.writeValueAsString(new EmbedRequestBody(fileName, content, spanBodies));

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(props.getBaseUrl() + "/embed"))
              .timeout(Duration.ofMillis(props.getEmbedRequestTimeoutMs()))
              .header("Content-Type", "application/json")
              .POST(BodyPublishers.ofString(body))
              .build();

      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        logger.warn(
            "AST service /embed returned HTTP {} for {}, skipping embeddings. Response body: {}",
            response.statusCode(),
            fileName,
            truncateForLog(response.body()));
        return List.of();
      }

      EmbedResponseWire wire = mapper.readValue(response.body(), EmbedResponseWire.class);

      if (!"ok".equals(wire.status())) {
        logger.warn(
            "AST service /embed reported status={} for {}: {}",
            wire.status(),
            fileName,
            wire.detail());
        return List.of();
      }

      return wire.embeddings().stream()
          .map(e -> new FunctionEmbedding(e.qualifiedName(), e.vector(), e.truncated()))
          .toList();
    } catch (IOException | InterruptedException | RuntimeException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      logger.warn(
          "AST service /embed call failed for {}, skipping embeddings: {}",
          fileName,
          e.getMessage());
      return List.of();
    }
  }
}
