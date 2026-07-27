package com.hackathon.platform.scoring.queue;

import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  private final SubmissionRepository subRepo;
  private final SolverVersionRepository solverVersionRepo;

  public String enqueue(Long submissionId) {
    setQueue(submissionId);
    var record =
        redis
            .opsForStream()
            .add(properties.getStreamKey(), Map.of("submissionId", String.valueOf(submissionId)));
    logger.info("enqueu submission {} as stream record {}", submissionId, record);
    return record != null ? record.getValue() : null;
  }

  public List<String> enqueueAllForHackathon(UUID hackathonId) {
    Long activeSolverVersionId =
        solverVersionRepo
            .findByHackathonIdAndIsActiveTrue(hackathonId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No active solver version found for hackathon " + hackathonId))
            .getId();

    subRepo.updateSolverVersionForHackathon(hackathonId, activeSolverVersionId);

    List<Long> submissionIds = subRepo.findIdsByHackathonId(hackathonId);
    List<String> records = new ArrayList<>(submissionIds.size());
    for (Long submissionId : submissionIds) {
      String record = enqueue(submissionId);
      if (record != null) {
        records.add(record);
      }
    }
    logger.info(
        "enqueued {} of {} submissions for hackathon {} rescore against solver version {}",
        records.size(),
        submissionIds.size(),
        hackathonId,
        activeSolverVersionId);
    return records;
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
