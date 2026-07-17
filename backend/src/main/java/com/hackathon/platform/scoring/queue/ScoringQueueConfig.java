package com.hackathon.platform.scoring.queue;

import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@Configuration
@RequiredArgsConstructor
public class ScoringQueueConfig{
    private static final Logger logger =LoggerFactory.getLogger(ScoringQueueConfig.class);
    private final StringRedisTemplate redis;
    private final ScoringQueueProperties properties;
    private final ScoringJobConsumer consumer;

    public void createConsumerGroup(){
        try{redis.opsForStream().createGroup(properties.getStreamKey(), ReadOffset.from("0"), properties.getConsumerGroup());
                logger.info("Created consumer group {} on stream no. {}", properties.getConsumerGroup(), properties.getStreamKey());
    } catch (Exception e){
            if(e.getMessage()!=null && e.getMessage().contains("BUSYGROUP")){
                logger.info("Consumer group {} exists", properties.getConsumerGroup());
            }else{ throw e;}
        }}

    @Bean(destoryMethod = "shutdown")
    public ExecutorService scoringStreamExecutor(){
        return Executors.newFixedThreadPool(properties.getConcurrency());
    }
}