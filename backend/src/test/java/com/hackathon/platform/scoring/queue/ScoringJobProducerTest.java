package com.hackathon.platform.scoring.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class ScoringJobProducerTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private StreamOperations<String, Object, Object> streamOps;
  @Mock private SubmissionRepository submissionRepo;
  @Mock private SolverVersionRepository solverVersionRepo;

  @InjectMocks private ScoringJobProducer producer;

  @Test
  void enqueue_marksSubmissionQueued_beforePushingToStream() {
    ScoringQueueProperties props = new ScoringQueueProperties();
    props.setStreamKey("scoring:jobs");

    producer = new ScoringJobProducer(redisTemplate, props, submissionRepo, solverVersionRepo);

    Submission sub = new Submission();
    sub.setId(42L);
    sub.setStatus("PENDING");
    when(submissionRepo.findById(42L)).thenReturn(Optional.of(sub));
    when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOps);
    when(streamOps.add(eq(props.getStreamKey()), any(Map.class))).thenReturn(RecordId.of("1-1"));

    producer.enqueue(42L);

    assertThat(sub.getStatus()).isEqualTo("QUEUED");
    verify(submissionRepo).save(sub);
    verify(streamOps).add(eq(props.getStreamKey()), eq(Map.of("submissionId", "42")));
  }

  @Test
  void enqueue_throwsIfSubmissionMissing() {
    ScoringQueueProperties props = new ScoringQueueProperties();
    producer = new ScoringJobProducer(redisTemplate, props, submissionRepo, solverVersionRepo);

    when(submissionRepo.findById(99L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> producer.enqueue(99L));
  }

  @Test
  void enqueueAllForHackathon_enqueuesAllSubmissionsWithActiveSolverVersion() {
    ScoringQueueProperties props = new ScoringQueueProperties();
    props.setStreamKey("scoring:jobs");

    producer = new ScoringJobProducer(redisTemplate, props, submissionRepo, solverVersionRepo);

    UUID hackathonId = UUID.randomUUID();
    Long activeSolverVersionId = 1L;
    Long submissionId1 = 100L;
    Long submissionId2 = 101L;

    com.hackathon.platform.model.SolverVersion solverVersion =
        new com.hackathon.platform.model.SolverVersion();
    solverVersion.setId(activeSolverVersionId);

    when(solverVersionRepo.findByHackathonIdAndIsActiveTrue(hackathonId))
        .thenReturn(Optional.of(solverVersion));

    Submission sub1 = new Submission();
    sub1.setId(submissionId1);
    Submission sub2 = new Submission();
    sub2.setId(submissionId2);

    when(submissionRepo.findById(submissionId1)).thenReturn(Optional.of(sub1));
    when(submissionRepo.findById(submissionId2)).thenReturn(Optional.of(sub2));
    when(submissionRepo.findIdsByHackathonId(hackathonId))
        .thenReturn(java.util.List.of(submissionId1, submissionId2));
    when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOps);
    when(streamOps.add(eq(props.getStreamKey()), any(Map.class)))
        .thenReturn(RecordId.of("1-1"))
        .thenReturn(RecordId.of("1-2"));

    java.util.List<String> records = producer.enqueueAllForHackathon(hackathonId);

    assertThat(records).hasSize(2);
    verify(submissionRepo).updateSolverVersionForHackathon(hackathonId, activeSolverVersionId);
    verify(submissionRepo).save(sub1);
    verify(submissionRepo).save(sub2);
    verify(streamOps, times(2)).add(eq(props.getStreamKey()), any(Map.class));
  }

  @Test
  void enqueueAllForHackathon_throwsWhenNoActiveSolverVersion() {
    ScoringQueueProperties props = new ScoringQueueProperties();
    producer = new ScoringJobProducer(redisTemplate, props, submissionRepo, solverVersionRepo);

    UUID hackathonId = UUID.randomUUID();

    when(solverVersionRepo.findByHackathonIdAndIsActiveTrue(hackathonId))
        .thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, () -> producer.enqueueAllForHackathon(hackathonId));
  }
}
