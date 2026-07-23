package com.hackathon.platform.scoring.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.lettuce.core.models.stream.PendingMessage;
import static org.springframework.data.domain.Range.unbounded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StreamOperations;

@Component
@RequiredArgsConstructor
public class ScoringJobReclaimer {
    private final ScoringJobConsumer cons;
    private final StringRedisTemplate redis;
    private final ScoringQueueProperties properties;
    private static final Logger logger = LoggerFactory.getLogger(ScoringJobReclaimer.class);

    private StreamOperations<String,String, String> streamOps(){
        return redis.opsForStream();
    }

    public void reclaimStuckJob(){
        PendingMessage p = redis.opsForStream().pending(properties.getStreamKey(), properties.getConsumerKey(), unbounded(), 100);
        if(p.isEmpty()){
            return;
        }
    }
}