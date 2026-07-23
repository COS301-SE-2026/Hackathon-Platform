package com.hackathon.platform.scoring.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import java.util.List;
import org.springframework.data.redis.connection.stream.PendingMessage;
import java.util.stream.Collectors;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ScoringJobReclaimer {
    private final StringRedisTemplate redis;
    private final ScoringQueueProperties properties;
    private static final Logger logger = LoggerFactory.getLogger(ScoringJobReclaimer.class);
    private final ScoringJobConsumer consumer;

    private StreamOperations<String,String, String> streamOps(){
        return redis.opsForStream();
    }

    public void reclaim(){
        PendingMessages p = streamOps().pending(properties.getStreamKey(), properties.getConsumerKey(), Range.unbounded(), 100);
        if(p.isEmpty()){
            return;
        }

        List<RecordId> stale = p.stream().filter(pm -> pm.getElapsedTimeSinceLastDelivery().toMillis() > properties.getPendingMinIdleMs()).map(PendingMessage::getId).collect(Collectors.toList());
        if(stale.isEmpty()){
            return;
        }

        logger.warn("Reclaiming {} stale sorcing job {}", stale.size(), stale);

        List<MapRecord<String, String, String>> claimed = streamOps().claim(properties.getStreamKey(), properties.getConsumerKey(), "reclaimer", XClaimOptions.minIdle(Duration.ofMillis(properties.getPendingMinIdleMs())).ids(stale));
        for(MapRecord<String,String, String> record:claimed){
            consumer.onMessage(record);
        }
    }
}