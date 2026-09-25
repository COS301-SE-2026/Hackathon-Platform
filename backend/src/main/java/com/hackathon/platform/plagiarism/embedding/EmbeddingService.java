package com.hackathon.platform.plagiarism.embedding;

import com.hackathon.platform.plagiarism.ast.AstFunctionSpan;
import com.hackathon.platform.plagiarism.ast.AstServiceClient;
import com.hackathon.platform.plagiarism.ast.FunctionEmbedding;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Turns "here are the function spans this submission's files contain" into stored pgvector rows.
 */
@Service
@RequiredArgsConstructor
public class EmbeddingService {

  private final AstServiceClient astClient;
  private final FunctionEmbeddingStore store;

  public void embedAndStore(
      Long submissionId,
      Map<String, String> filesByName,
      Map<String, List<AstFunctionSpan>> functionsByFile) {
    List<FunctionEmbeddingStore.Row> rows = new ArrayList<>();

    for (var entry : functionsByFile.entrySet()) {
      String fileName = entry.getKey();
      List<AstFunctionSpan> spans = entry.getValue();
      if (spans.isEmpty()) {
        continue;
      }

      String content = filesByName.get(fileName);
      if (content == null) {
        continue;
      }

      List<FunctionEmbedding> embeddings = astClient.embed(fileName, content, spans);
      for (FunctionEmbedding e : embeddings) {
        rows.add(
            new FunctionEmbeddingStore.Row(fileName, e.qualifiedName(), e.vector(), e.truncated()));
      }
    }

    // Replace even when empty.
    store.replaceEmbeddings(submissionId, rows);
  }
}
