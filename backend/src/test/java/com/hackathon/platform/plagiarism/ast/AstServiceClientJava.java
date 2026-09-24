package com.hackathon.platform.plagiarism.ast;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AstServiceClientTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {

    if (server != null) {
      server.stop(0);
    }

  }

  private PlagiarismAstProperties propsFor(HttpServer started) {
    PlagiarismAstProperties props = new PlagiarismAstProperties();
    props.setBaseUrl("http://localhost:" + started.getAddress().getPort());
    props.setConnectTimeoutMs(1000);
    props.setRequestTimeoutMs(1000);
    props.setEmbedRequestTimeoutMs(1000);
    return props;

  }

  private HttpServer startServer(String path, int status, String responseBody) throws IOException {

    HttpServer s = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    s.createContext(
        path,
        exchange -> {

          byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    s.start();
    return s;

  }

  @Test
  void parse_serviceDisabled_returnsServiceUnavailableWithoutCallingNetwork() {

    PlagiarismAstProperties props = new PlagiarismAstProperties();
    props.setEnabled(false);
    AstServiceClient client = new AstServiceClient(props);

    AstParseResponse response = client.parse("Main.java", "class Main {}");

    assertThat(response.status()).isEqualTo("service_unavailable");
    assertThat(response.isOk()).isFalse();
    assertThat(response.detail()).isEqualTo("ast service disabled");

  }

  @Test
  void parse_serviceReturnsOk_deserializesTokensAndFunctions() throws IOException {

    String body =
        "{\"status\":\"ok\",\"language\":\"java\","
            + "\"tokens\":[{\"text\":\"ID\",\"start\":0,\"end\":2}],"
            + "\"functions\":[{\"qualified_name\":\"Main.run\",\"node_type\":\"method\","
            + "\"start_byte\":0,\"end_byte\":10,\"start_line\":1,\"end_line\":2}]}";
    server = startServer("/parse", 200, body);

    AstServiceClient client = new AstServiceClient(propsFor(server));
    AstParseResponse response = client.parse("Main.java", "class Main {}");

    assertThat(response.isOk()).isTrue();
    assertThat(response.language()).isEqualTo("java");
    assertThat(response.tokens()).hasSize(1);
    assertThat(response.tokens().get(0).text()).isEqualTo("ID");
    assertThat(response.functions()).hasSize(1);
    assertThat(response.functions().get(0).qualifiedName()).isEqualTo("Main.run");


  }

  @Test
  void parse_serviceReturnsUnsupportedLanguage_isNotOk() throws IOException {

    server = startServer("/parse", 200, "{\"status\":\"unsupported_language\",\"tokens\":[],\"functions\":[]}");

    AstServiceClient client = new AstServiceClient(propsFor(server));
    AstParseResponse response = client.parse("script.rb", "puts 1");

    assertThat(response.isOk()).isFalse();
    assertThat(response.status()).isEqualTo("unsupported_language");

  }

  @Test
  void parse_serviceReturnsNon200_fallsBackToServiceUnavailable() throws IOException {

    server = startServer("/parse", 500, "{\"error\":\"boom\"}");

    AstServiceClient client = new AstServiceClient(propsFor(server));
    AstParseResponse response = client.parse("Main.java", "class Main {}");

    assertThat(response.status()).isEqualTo("service_unavailable");
    assertThat(response.detail()).isEqualTo("http 500");

  }

  @Test 
  void parse_connectionRefused_fallsBackToServiceUnavailable() {

    PlagiarismAstProperties props = new PlagiarismAstProperties();
    props.setBaseUrl("http://localhost:1");
    props.setConnectTimeoutMs(500);
    AstServiceClient client = new AstServiceClient(props);

    AstParseResponse response = client.parse("Main.java", "class Main {}");

    assertThat(response.status()).isEqualTo("service_unavailable");
    assertThat(response.functions()).isEmpty();
    assertThat(response.tokens()).isEmpty();

  }

  @Test
  void parse_malformedJsonResponse_fallsBackToServiceUnavailable() throws IOException {

    server = startServer("/parse", 200, "not json at all {{{");

    AstServiceClient client = new AstServiceClient(propsFor(server));
    AstParseResponse response = client.parse("Main.java", "class Main {}");

    assertThat(response.status()).isEqualTo("service_unavailable");

  }

  @Test 
  void embed_embeddingDisabled_returnsEmptyListWithoutCallingNetwork() {

    PlagiarismAstProperties props = new PlagiarismAstProperties();
    props.setEmbeddingEnabled(false);
    AstServiceClient client = new AstServiceClient(props);

    List<FunctionEmbedding> result =
        client.embed("Main.java", "content", List.of(new AstFunctionSpan("Main.run", "method", 0, 5, 1, 1)));

    assertThat(result).isEmpty();

  }



}