package com.hackathon.platform.plagiarism.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.plagiarism.ast.AstFunctionSpan;
import com.hackathon.platform.plagiarism.ast.AstServiceClient;
import com.hackathon.platform.plagiarism.ast.FunctionEmbedding;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

  @Mock private AstServiceClient astClient;
  @Mock private FunctionEmbeddingStore store;

  private EmbeddingService service;

  private AstFunctionSpan span(String qualifiedName) {
    return new AstFunctionSpan(qualifiedName, "function_definition", 0, 10, 1, 2);
  }

  @Test
  void embedAndStore_skipsFileWithNoFunctionSpans(){

    service = new EmbeddingService(astClient, store);

    Map<String, String> files = Map.of("Empty.java", "class Empty {}");
    Map<String, List<AstFunctionSpan>> functions = Map.of("Empty.java", List.of());

    service.embedAndStore(1L, files, functions);

    verify(astClient, never()).embed(anyString(), anyString(), any());
    verify(store).replaceEmbeddings(eq(1L), eq(List.of()));

  }

  @Test
  void embedAndStore_skipsFunctionsWhoseFileContentIsMissing(){

    service = new EmbeddingService(astClient, store);

    Map<String, String> files = Map.of();
    Map<String, List<AstFunctionSpan>> functions = Map.of("Main.java", List.of(span("Main.run")));

    service.embedAndStore(1L, files, functions);

    verify(astClient, never()).embed(anyString(), anyString(), any());
    verify(store).replaceEmbeddings(eq(1L), eq(List.of()));

  }

  @Test
  void embedAndStore_callsClientAndMapsResultsIntoStoreRows(){

    service = new EmbeddingService(astClient, store);

    Map<String, String> files = Map.of("Main.java", "class Main { void run() {} }");
    List<AstFunctionSpan> spans = List.of(span("Main.run"));
    Map<String, List<AstFunctionSpan>> functions = Map.of("Main.java", spans);

    when(astClient.embed("Main.java", files.get("Main.java"), spans))
        .thenReturn(List.of(new FunctionEmbedding("Main.run", new float[] {0.1f, 0.2f}, false)));

    service.embedAndStore(1L, files, functions);

    ArgumentCaptor<List<FunctionEmbeddingStore.Row>> captor = ArgumentCaptor.forClass(List.class);
    verify(store).replaceEmbeddings(eq(1L), captor.capture());

    List<FunctionEmbeddingStore.Row> rows = captor.getValue();
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).fileName()).isEqualTo("Main.java");
    assertThat(rows.get(0).qualifiedName()).isEqualTo("Main.run");
    assertThat(rows.get(0).vector()).containsExactly(0.1f, 0.2f);
    assertThat(rows.get(0).truncated()).isFalse();

  }


}
