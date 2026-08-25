package com.hackathon.platform.dto;

import java.time.Instant;

public class SubmissionRateBucket {

  private Instant bucketStart;
  private long count;

  public SubmissionRateBucket() {}

  public SubmissionRateBucket(Instant bucketStart, long count) {
    this.bucketStart = bucketStart;
    this.count = count;
  }

  public Instant getBucketStart() {
    return bucketStart;
  }

  public long getCount() {
    return count;
  }
}
