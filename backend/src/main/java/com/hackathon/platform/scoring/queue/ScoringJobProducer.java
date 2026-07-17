package com.hackathon.platform.scoring.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ScoringJobProducer{
    private final StringRedisTemplate redis;
    private final ScoringQueueProperties properties;
    private static final Logger logger = LoggerFactory.getLogger(ScoringJobProducer.class);

    public String enqueue(Long submissionId){
        var record = redis.opsForStream().add(properties.getStreamKey(), Map.of("submissionId", String.valueOf(submissionId)));
        logger.info("enqueu submission {} as stream record {}", submissionId, record);
        return record!=null ? record.getValue() : null;
    }
}