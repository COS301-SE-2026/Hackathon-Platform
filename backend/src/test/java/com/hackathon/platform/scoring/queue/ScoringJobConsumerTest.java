package com.hackathon.platform.scoring.queue;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hackathon.platform.scoring.ScoringService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class ScoringJobConsumerTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ScoringService scoringService;
  @Mock private StreamOperations<String, Object, Object> streamOps;

  @InjectMocks private ScoringJobConsumer consumer;

  @Test
  void onMessage_acksAfterSuccessfulScoring() {
    ScoringQueueProperties props = new ScoringQueueProperties();
    props.setConsumerKey("scoring-group");

    consumer = new ScoringJobConsumer(redisTemplate, props, scoringService);
    when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOps);

    MapRecord<String, String, String> record =
        MapRecord.create("scoring:jobs", Map.of("submissionId", "7")).withId(RecordId.of("1-1"));

    consumer.onMessage(record);

    verify(scoringService).scoreSubmission(7L);
    verify(streamOps).acknowledge(eq(props.getConsumerKey()), eq(record));
  }

  @Test
  void onMessage_doesNotAck_whenScoringThrowsUnexpectedException() {
    ScoringQueueProperties props = new ScoringQueueProperties();
    props.setConsumerKey("scoring-group");

    consumer = new ScoringJobConsumer(redisTemplate, props, scoringService);
    doThrow(new RuntimeException("DB connection lost")).when(scoringService).scoreSubmission(7L);

    MapRecord<String, String, String> record =
        MapRecord.create("scoring:jobs", Map.of("submissionId", "7")).withId(RecordId.of("1-1"));

    consumer.onMessage(record);

    verify(scoringService).scoreSubmission(7L);
    verify(redisTemplate, never()).opsForStream();
  }

  @Test
  void onMessage_acksAndDrops_malformedRecordMissingSubmissionId() {
    ScoringQueueProperties props = new ScoringQueueProperties();
    props.setConsumerKey("scoring-group");

    consumer = new ScoringJobConsumer(redisTemplate, props, scoringService);
    when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOps);

    MapRecord<String, String, String> record =
        MapRecord.create("scoring:jobs", Map.<String, String>of()).withId(RecordId.of("1-1"));

    consumer.onMessage(record);

    verifyNoInteractions(scoringService);

    verify(streamOps).acknowledge(eq(props.getConsumerKey()), eq(record));
  }

  @Test
  void onMessage_handlesNullSubmissionId() {
    ScoringQueueProperties props = new ScoringQueueProperties();
    props.setConsumerKey("scoring-group");

    consumer = new ScoringJobConsumer(redisTemplate, props, scoringService);

    MapRecord<String, String, String> record =
        MapRecord.create("scoring:jobs", Map.of("submissionId", "invalid"))
            .withId(RecordId.of("1-1"));

    org.junit.jupiter.api.Assertions.assertThrows(
        NumberFormatException.class, () -> consumer.onMessage(record));

    verifyNoInteractions(scoringService);
    verify(redisTemplate, never()).opsForStream();
  }
}
