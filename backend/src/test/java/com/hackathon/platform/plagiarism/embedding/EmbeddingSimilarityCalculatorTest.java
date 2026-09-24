package com.hackathon.platform.plagiarism.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.hackathon.platform.plagiarism.embedding.EmbeddingSimilarityCalculator.FunctionMatch;
import com.hackathon.platform.plagiarism.embedding.FunctionEmbeddingStore.StoredEmbedding;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EmbeddingSimilarityCalculatorTest {

  private final EmbeddingSimilarityCalculator calculator = new EmbeddingSimilarityCalculator();

  @Test
  void cosineSimilarity_identicalVectors_returnsOne() {

    float[] a = {1f, 2f, 3f};
    float[] b = {1f, 2f, 3f};

    assertThat(calculator.cosineSimilarity(a, b)).isCloseTo(1.0, within(1e-9));

  }

  @Test
  void cosineSimilarity_orthogonalVectors_returnsZero() {

    float[] a = {1f, 0f};
    float[] b = {0f, 1f};

    assertThat(calculator.cosineSimilarity(a, b)).isCloseTo(0.0, within(1e-9));
    
  }

  @Test
  void cosineSimilarity_oppositeVectors_returnsNegativeZero() {

    float[] a = {1f, 2f, 3f};
    float[] b = {-1f, -2f, -3f};

    assertThat(calculator.cosineSimilarity(a, b)).isCloseTo(-1.0, within(1e-9));
    
  }

  @Test
  void cosineSimilarity_zeroVectors_returnsZeroRatherThanNaN() {

    float[] a = {0f, 0f, 0f};
    float[] b = {1f, 2f, 3f};

    assertThat(calculator.cosineSimilarity(a, b)).isEqualTo(0.0);

  }

  @Test
  void meanVector_emptyList_returnsEmptyArray() {

    assertThat(calculator.meanVector(List.of())).isEmpty();

  }

  @Test
  void meanVector_singleVector_returnsZeroVectorSameDimension() {

    float[] result = calculator.meanVector(List.of(new float[] {5f, 7f}));

    assertThat(result).containsExactly(0f, 0f);
  }
}