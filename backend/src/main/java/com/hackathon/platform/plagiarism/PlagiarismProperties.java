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

  /** Jaccard similarity (0-1) on structural fingerprints at/above which a pair is auto-flagged. */
  private double flagThreshold = 0.6;

  /** Minimum token count a submission needs to be worth comparing at all (skips trivial stubs). */
  private int minTokenCount = 20;

  private Queue queue = new Queue();

  @Data
  public static class Queue {
    
    private String streamKey = "plagiarism:jobs";
    private String consumerKey = "plagiarism-workers";
    private int concurrency = 2;
    private long pollTimeoutMs = 2000;

  }
}
