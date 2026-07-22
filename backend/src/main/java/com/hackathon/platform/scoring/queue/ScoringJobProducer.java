package com.hackathon.platform.scoring.queue;

import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.SubmissionRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScoringJobProducer {
  private final StringRedisTemplate redis;
  private final ScoringQueueProperties properties;
  private static final Logger logger = LoggerFactory.getLogger(ScoringJobProducer.class);
  private SubmissionRepository subRepo;

  public String enqueue(Long submissionId) {
    setQueue(submissionId);
    var record =
        redis
            .opsForStream()
            .add(properties.getStreamKey(), Map.of("submissionId", String.valueOf(submissionId)));
    logger.info("enqueu submission {} as stream record {}", submissionId, record);
    return record != null ? record.getValue() : null;
  }

  @Transactional
  public void setQueue(Long id) {
    Submission sub =
        subRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("submission not found"));
    sub.setStatus("QUEUED");
    subRepo.save(sub);
  }
}
