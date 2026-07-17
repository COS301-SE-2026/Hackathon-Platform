package com.hackathon.platform.scoring.queue;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="scoring.queue")
@Data
public class ScoringQueueProperties{

    @Getter
    @Setter
    private String streamKey = "scoring:jobs";

    @Getter
    @Setter
    private String consumerKey = "scoring-workers";

    @Getter
    @Setter
    private int concurrency = 10;

    @Getter
    @Setter
    private long pollTimeoutMs = 2000;
    @Getter
    @Setter
    private long pendingMinIdleMs = 60000;
}