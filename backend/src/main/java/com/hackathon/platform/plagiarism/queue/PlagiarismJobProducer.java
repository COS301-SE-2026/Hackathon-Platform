package com.hackathon.platform.plagiarism.queue;

import com.hackathon.platform.model.PlagiarismRun;
import com.hackathon.platform.plagiarism.PlagiarismProperties;
import com.hackathon.platform.repository.PlagiarismRunRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlagiarismJobProducer {
  private static final Logger logger = LoggerFactory.getLogger(PlagiarismJobProducer.class);

  private final StringRedisTemplate redis;
  private final PlagiarismProperties properties;
  private final PlagiarismRunRepository runRepo;

  /** Creates Plagiarism row and enqueues id for consumer to pick up */
  public Long enqueue(UUID eventId, Short levelId, int topN, UUID requestedBy) {
    PlagiarismRun run = runRepo.save(new PlagiarismRun(eventId, levelId, topN, requestedBy));

    var record =
        redis
            .opsForStream()
            .add(
                properties.getQueue().getStreamKey(), Map.of("runId", String.valueOf(run.getId())));
    logger.info("enqueued plagiarism run {} as stream record {}", run.getId(), record);

    return run.getId();
  }
}
