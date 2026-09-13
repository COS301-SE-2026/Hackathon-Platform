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

  private static final long BASE = 101; //polynomial rolling hash base

  public FingerprintResult fingerprint(List<String> tokens, int kgramSize, int windowSize) {
    if(tokens == null || tokens.size() < kgramSize) {
        return new FingerprintResult(tokens == null ? 0 : tokens.size(), Set.of());

    }

    long[] kgramHashes = hashKgrams(tokens, kgramSize);
    Set<Fingerprint> selected = new LinkedHashSet<>();

    ArrayDeque<Integer> window = new ArrayDeque<>();
    int lastSelectedPos = -1;
    for(int i = 0; i < kgramHashes.length; i++) {
        while (!window.isEmpty() && kgramHashes[window.peekLast()] >= kgramHashes[i]) {
            window.pollFirst();

        }

        window.addLast(i);
        while (window.peekFirst() <= i - windowSize) {
            window.pollFirst();
        }

        if(i >= windowSize - 1) {
            int minPos =  window.peekFirst();
            if(minPos != lastSelectedPos) {
                selected.add(new Fingerprint(kgramHashes[minPos], minPos));
                lastSelectedPos = minPos;
            }
        }
    }
    return new FingerprintResult(tokens.size(), selected);

  }

  private long[] hashKgrams(List<String> tokens, int k) {

    int n = tokens.size() - k + 1;
    long[] hashes =  new long[n];

    for(int i = 0; i < n; i++) {
        long h = 0;
        for(int j = 0; j < k; j++) {
            h = h * BASE + tokens.get(i + j).hashCode();

        }
        hashes[i] = h;
    }
    return hashes;

  }

  /** Jaccard similarity between two fingerprint hash sets */
  public double Jaccard(Set<Long> a, Set<Long> b) {

    if(a.isEmpty() && b.isEmpty()) {
        return 0.0;

    }

    long intersection = a.stream().filter(b::contains).count();
    long union =  a.size() + b.size() - intersection;
    return union == 0 ? 0.0 : (double) intersection / union;
    
  }


}