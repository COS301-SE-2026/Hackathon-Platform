package com.hackathon.platform.scoring.queue;

import org.springframework.stereotype.Component;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.RequiredArgsConstructor;
import com.hackathon.platform.scoring.ScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
@RequiredArgsConstructor
public class ScoringJobConsumer implements StreamListener<String,MapRecord<String,String,String>> {
    private final StringRedisTemplate redis;
    private final ScoringQueueProperties properties;
    private static final Logger logger = LoggerFactory.getLogger(ScoringJobConsumer.class);
    private final ScoringService scorer;

    @Override
    public void onMessage(MapRecord<String, String, String> msg) {
        String submission = msg.getValue().get("SubmissionId");
        Long submissionId = submission != null ? Long.valueOf(submission) : null;

        if (submissionId == null) {
            logger.error("something wrong with id {} will be dropped", msg.getId());
            ack(msg);
            return;
        }
        try {
            logger.info("got submission {} from record {}", submissionId, msg.getId());
            scorer.scoreSubmission(submissionId);
            ack(msg);
        } catch (Exception e) {
            logger.error("error scoring submission {}, will retry", submissionId, msg.getId(), e);
        }
    }


    private void ack(MapRecord<String, String, String> record) {
        redis.opsForStream().acknowledge(properties.getConsumerGroup(), record);
    }
}