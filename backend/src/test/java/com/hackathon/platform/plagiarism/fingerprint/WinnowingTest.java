package com.hackathon.platform.plagiarism.fingerprint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.hackathon.platform.plagiarism.fingerprint.Winnowing.FingerprintResult;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WinnowingTest {

  private final Winnowing winnowing = new Winnowing();

  @Test
  void fingerprint_identicalTokenStreams_produceIdenticalFingerprints() {

    List<String> tokens = List.of("a", "b", "c", "d", "e", "f", "g", "h");

    FingerprintResult first =  winnowing.fingerprint(tokens, 3, 2);
    FingerprintResult second = winnowing.fingerprint(tokens, 3, 2);

    assertThat(first.hashes()).isEqualTo(second.hashes());
    assertThat(winnowing.jaccard(first.hashes(), second.hashes())).isEqualTo(1.0);

  }

  @Test
  void fingerprint_shorterThanKgramSize_returnsEmptyFingerprintSet() {

    List<String> tokens = List.of("a", "b");

    FingerprintResult result = winnowing.fingerprint(tokens, 5, 2);

    assertThat(result.fingerprints()).isEmpty();
    assertThat(result.tokenCount()).isEqualTo(2);

  }

  @Test
  void fingerprint_nullTokenList_returnsEmptyFingerprintSetWithZeroCount() {

    FingerprintResult result = winnowing.fingerprint(null, 5, 2);

    assertThat(result.fingerprints()).isEmpty();
    assertThat(result.tokenCount()).isEqualTo(0);

  }

  @Test
  void fingerprint_completelyDifferentTokenStreams_haveLowJaccardSimilarity() {

    List<String> tokensA = List.of("int", "x", "=", "1", ";", "return", "x", ";");
    List<String> tokensB = List.of("string", "greet", "=", "\"hi\"", ";", "print", "greet", ";", "end");

    FingerprintResult fpA = winnowing.fingerprint(tokensA, 3, 2);
    FingerprintResult fpB = winnowing.fingerprint(tokensB, 3, 2);

    double similarity = winnowing.jaccard(fpA.hashes(), fpB.hashes());

    assertThat(similarity).isLessThan(0.5);

  }

  @Test
  void fingerprint_renamedIdentifiers_stillMatchAfterNormalization() {

    List<String> tokensA = List.of("for", "(", "ID", "=", "0", ";", "ID", "<", "N", ";", "ID", "++", ")");
    List<String> tokensB = List.of("for", "(", "ID", "=", "0", ";", "ID", "<", "N", ";", "ID", "++", ")");

    FingerprintResult fpA = winnowing.fingerprint(tokensA, 4, 3);
    FingerprintResult fpB = winnowing.fingerprint(tokensB, 4, 3);

    assertThat(winnowing.jaccard(fpA.hashes(), fpB.hashes())).isEqualTo(1.0);
    
  }

}