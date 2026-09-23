package com.hackathon.platform.plagiarism.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hackathon.platform.model.PlagiarismRun;
import com.hackathon.platform.plagiarism.PlagiarismProperties;
import com.hackathon.platform.repository.PlagiarismRunRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class PlagiarismJobProducerTest {

  @Mock private StringRedisTemplate redis;
  @Mock private StreamOperations<String, Object, Object> streamOps;
  @Mock private PlagiarismRunRepository runRepo;

  private PlagiarismJobProducer producer;

  @Test
  void enqueue_savesRunThenPushesRunIdToStream() {

    PlagiarismProperties props = new PlagiarismProperties();
    props.getQueue().setStreamKey("plagiarism:jobs");

    producer = new PlagiarismJobProducer(redis, props, runRepo);

    UUID eventId = UUID.randomUUID();
    Short levelId = (short) 3;
    UUID requestedBy = UUID.randomUUID();

    when(runRepo.save(any(PlagiarismRun.class)))
        .thenAnswer(
            invocation -> {

                PlagiarismRun run = invocation.getArgument(0);
                java.lang.reflect.Field idField = PlagiarismRun.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(run, 55L);
                return run;

            }
        );

    when(redis.opsForStream()).thenReturn((StreamOperations) streamOps);
    when(streamOps.add(eq(props.getQueue().getStreamKey()), any(Map.class)))
        .thenReturn(RecordId.of("1-1"));

    Long runId = producer.enqueue(eventId, levelId, 25, requestedBy);

    assertThat(runId).isEqualTo(55L);
    verify(runRepo).save(any(PlagiarismRun.class));
    verify(streamOps).add(eq(props.getQueue().getStreamKey()), eq(Map.of("runId", "55")));

  }

}