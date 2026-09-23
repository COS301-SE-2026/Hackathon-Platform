package com.hackathon.platform.plagiarism;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Config for the batch plagiarism check.
 * for the top N teams on a leaderboard.
 */
@Component
@ConfigurationProperties(prefix = "plagiarism")
@Data
public class PlagiarismProperties {

  /** Default number of top-leaderboard teams per level to include when not specified by caller. */
  private int defaultTopN = 20;

  /** k-gram size (number of normalized tokens per shingle) used for winnowing fingerprints. */
  private int kgramSize = 15;

  /** Winnowing guarantee window size (in k-grams). Smaller = more fingerprints, more sensitive. */
  private int windowSize = 6;

  /** Jaccard similarity (0-1) on structural fingerprints at/above which a pair is auto-flagged.
   * Applies to combined score when embedding signal and score available
   */
  private double flagThreshold = 0.6;

  /** Minimum token count a submission needs to be worth comparing at all (skips trivial stubs). */
  private int minTokenCount = 20;

  /**
   * Weight given to the semantic (embedding).
   */
  private double embeddingWeight = 0.4;

  /**When true, a pair is also flagged is its combined score is a stat outlier relative to every other compared pair in the same level. */
  private boolean useRelativeThreshold = true;

  /**How many standard deviations above the levels mean combined score counts as outlier */
  private double relativeThresholdZScore = 2.0;

  /**
   * A level needs at least this many compared before trusting realtive threshold.
   */
  private int getMinPairsForRelativeThreshold = 5;

  /**Minimun (corpus-centered) cosine similarity for a function pair to be surfaced in the diff view's function match list */
  private double functionMatchThreshold = 0.75;

  /** Caps how many function matches the diff view returns, to keep UI reabdle */
  private int maxFunctionMatches = 15;

  private Queue queue = new Queue();

  @Data
  public static class Queue {
    
    private String streamKey = "plagiarism:jobs";
    private String consumerKey = "plagiarism-workers";
    private int concurrency = 2;
    private long pollTimeoutMs = 2000;

  }
}
