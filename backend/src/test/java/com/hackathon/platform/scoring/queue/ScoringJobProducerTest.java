package com.hackathon.platform.scoring.queue;

import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    when(streamOps.add(eq(props.getStreamKey()), any(Map.class)))
        .thenReturn(RecordId.of("1-1"));

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

  
}