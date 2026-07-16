package com.hackathon.platform.scoring.queue;

import org.springframework.stereotype.Component;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;

@Component
public class ScoringJobConsumer implements StreamListener<String,MapRecord<String,String,String>>{
    private final StringRedisTemplate redis;
    private final ScoringQueueProperties properties;

    private void ack(MapRecord<String, String, String> record){
        redis.opsForStream().acknowledge(properties.getConsumerGroup(), record);
    }
}