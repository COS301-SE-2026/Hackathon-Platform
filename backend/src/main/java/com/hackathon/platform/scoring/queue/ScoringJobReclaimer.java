package com.hackathon.platform.scoring.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.connection.stream.PendingMessages;

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

    public void reclaim(){
        PendingMessages p = streamOps().pending(properties.getStreamKey(), properties.getConsumerKey(), Range.unbounded(), 100);
        if(p.isEmpty()){
            return;
        }
    }
}