package com.hackathon.platform.plagiarism.queue;

import com.hackathon.platform.plagiarism.PlagiarismCheckService;
import com.hackathon.platform.plagiarism.PlagiarismProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlagiarismJobConsumer
    implements StreamListener<String, MapRecord<String, String, String>> {
  private static final Logger logger = LoggerFactory.getLogger(PlagiarismJobConsumer.class);

  private final StringRedisTemplate redis;
  private final PlagiarismProperties properties;
  private final PlagiarismCheckService checkService;

  @Override
  public void onMessage(MapRecord<String, String, String> msg) {
    String runIdStr = msg.getValue().get("runId");
    Long runId = runIdStr != null ? Long.valueOf(runIdStr) : null;

    if(runId == null) {
        logger.error("Plagiarism job record {} missing runId, dropping", msg.getId());
        ack(msg);
        return;

    }
    try {
        logger.info("starting plagiarism run {} from record {}", runId, msg.getId());
        checkService.execute(runId);
        ack(msg);
    } catch (Exception e) {
        logger.error("error running plagiarism check for run {}, will retry", runId, e);

    }
  }

  private void ack(MapRecord<String, String, String> record) {
    redis.opsForStream().acknowledge(properties.getQueue().getConsumerKey(), record);
    
  }


}