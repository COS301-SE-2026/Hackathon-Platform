package com.hackathon.platform.scoring.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.lettuce.core.models.stream.PendingMessage;
import static org.springframework.data.domain.Range.unbounded;

@Component
@RequiredArgsConstructor
public class ScoringJobReclaimer {
    private final ScoringJobConsumer cons;
    private final StringRedisTemplate redis;
    private final ScoringQueueProperties properties;

    public void reclaimStuckJob(){
        PendingMessage p = redis.opsForStream().pending(properties.getStreamKey(), properties.getConsumerKey(), unbounded(), 100);
        if(p.isEmpty()){
            return;
        }
    }
}