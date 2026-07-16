package com.hackathon.platform.scoring.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScoringJobProducer{
    private final StringRedisTemplate redis;
    private final ScoringQueueProperties properties;

    public String enquue(){
        var record = redis.opsForStream().add(properties.getStreamKey());
        return record.getValue();
    }
}