package com.hackathon.platform.scoring.queue;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="scoring.queue")
@Data
public class ScoringQueueProperties{
    private String streamKey = "scoring:jobs";
    private String consumerKey = "scoring-workers";
    private int concurrency = 10;
    private long pollTimeoutMs = 2000;
    private long pendingMinIdleMs = 60000;
}