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

  @Test
  void meanVector_multipleVectors_returnsComponentWiseAverage() {

    List<float[]> vectors = List.of(new float[] {2f, 4f}, new float[] {4f, 8f});

    float[] result = calculator.meanVector(vectors);

    assertThat(result).containsExactly(3f, 6f);
  }

  @Test
  void centerAll_subtractsMeanFromEveryVector() {

    List<StoredEmbedding> embeddings =
        List.of(
            new StoredEmbedding(1L, "a.foo", new float[] {2f, 4f}),
            new StoredEmbedding(1L, "a.bar", new float[] {4f, 8f}));
    float[] mean = {3f, 6f};

    List<StoredEmbedding> centered = calculator.centerAll(embeddings, mean);

    assertThat(centered).hasSize(2);
    assertThat(centered.get(0).vector()).containsExactly(-1f, -2f);
    assertThat(centered.get(1).vector()).containsExactly(1f, 2f);

    assertThat(centered.get(0).submissionId()).isEqualTo(1L);
    assertThat(centered.get(0).qualifiedName()).isEqualTo("a.foo");
  }

  @Test
  void centerAll_emptyMeanVector_returnsVectorsUnchanged() {
    List<StoredEmbedding> embeddings =
        List.of(new StoredEmbedding(1L, "a.foo", new float[] {2f, 4f}));

    List<StoredEmbedding> centered = calculator.centerAll(embeddings, new float[0]);

    assertThat(centered.get(0).vector()).containsExactly(2f, 4f);
  }

  @Test
  void symmetricBestMatch_eitherSideEmpty_returnsEmptyOptional() {

    List<StoredEmbedding> a = List.of(new StoredEmbedding(1L, "a.foo", new float[] {1f, 0f}));

    assertThat(calculator.symmetricBestMatch(a, List.of())).isEqualTo(Optional.empty());
    assertThat(calculator.symmetricBestMatch(List.of(), a)).isEqualTo(Optional.empty());
  }

  @Test
  void symmetricBestMatch_identicalSubmissions_returnsOne() {

    List<StoredEmbedding> a =
        List.of(
            new StoredEmbedding(1L, "a.foo", new float[] {1f, 0f}),
            new StoredEmbedding(1L, "a.bar", new float[] {0f, 1f}));

    List<StoredEmbedding> b =
        List.of(
            new StoredEmbedding(2L, "b.foo", new float[] {1f, 0f}),
            new StoredEmbedding(2L, "b.bar", new float[] {0f, 1f}));

    Optional<Double> result = calculator.symmetricBestMatch(a, b);

    assertThat(result).isPresent();
    assertThat(result.get()).isCloseTo(1.0, within(1e-9));
  }

  @Test
  void symmetricBestMatch_isAverageOfBothDirectionalBestMatches() {

    List<StoredEmbedding> a = List.of(new StoredEmbedding(1L, "a.foo", new float[] {1f, 0f}));
    List<StoredEmbedding> b =
        List.of(
            new StoredEmbedding(2L, "b.foo", new float[] {1f, 0f}),
            new StoredEmbedding(2L, "b.bar", new float[] {0f, 1f}));

    Optional<Double> result = calculator.symmetricBestMatch(a, b);

    assertThat(result).isPresent();
    assertThat(result.get()).isCloseTo(0.75, within(1e-9));
  }

  @Test
  void topFunctionMatches_filtersBelowThresholdAndSortsDescending() {

    List<StoredEmbedding> a =
        List.of(
            new StoredEmbedding(1L, "a.close", new float[] {1f, 0f}),
            new StoredEmbedding(1L, "a.far", new float[] {0f, 1f}));
    List<StoredEmbedding> b = List.of(new StoredEmbedding(2L, "b.target", new float[] {1f, 0f}));

    List<FunctionMatch> matches = calculator.topFunctionMatches(a, b, 0.5, 10);

    assertThat(matches).hasSize(1);
    assertThat(matches.get(0).qualifiedNameA()).isEqualTo("a.close");
    assertThat(matches.get(0).qualifiedNameB()).isEqualTo("b.target");
    assertThat(matches.get(0).similarity()).isCloseTo(1.0, within(1e-9));
  }

  @Test
  void topFunctionMatches_capsResultsAtMaxResults() {

    List<StoredEmbedding> a =
        List.of(
            new StoredEmbedding(1L, "a.one", new float[] {1f, 0f}),
            new StoredEmbedding(1L, "a.two", new float[] {1f, 0f}),
            new StoredEmbedding(1L, "a.three", new float[] {1f, 0f}));

    List<StoredEmbedding> b = List.of(new StoredEmbedding(2L, "b.target", new float[] {1f, 0f}));

    List<FunctionMatch> matches = calculator.topFunctionMatches(a, b, 0.0, 2);

    assertThat(matches).hasSize(2);
  }

  @Test
  void topFunctionMatches_noPairsMeetThreshold_returnsEmptyList() {

    List<StoredEmbedding> a = List.of(new StoredEmbedding(1L, "a.foo", new float[] {1f, 0f}));
    List<StoredEmbedding> b = List.of(new StoredEmbedding(2L, "b.foo", new float[] {0f, 1f}));

    List<FunctionMatch> matches = calculator.topFunctionMatches(a, b, 0.9, 10);

    assertThat(matches).isEmpty();
  }
}
