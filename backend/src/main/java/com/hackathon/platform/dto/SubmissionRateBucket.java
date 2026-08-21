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

    public void setBucketStart(Instant bucketStart) {
        this.bucketStart = bucketStart;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }


}