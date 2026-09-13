package com.hackathon.platform.plagiarism.fingerprint;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Implements winnowing (Schleimer, Wilkerson, Aiken 2003) over a normalized token sequence.
 *
 * The resulting fingerprint sets are compared pairwise with Jaccard similarity, which is fast
 * (near O(n) per comparison) and is the standard, defensible approach for code-plagiarism
 * detection.
 */
@Component
public class Winnowing {

    /** A fingerprint hash together with the token index it was selected at (for diff highlighting). */
  public record Fingerprint(long hash, int position) {}

  public record FingerprintResult(int tokenCount, Set<Fingerprint> fingerprints) {
    public Set<Long> hashes() {

      Set<Long> hashes = new LinkedHashSet<>();
      for (Fingerprint f : fingerprints) {
        hashes.add(f.hash());

      }
  
      return hashes;
    }
  }


}