package com.hackathon.platform.plagiarism.queue;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hackathon.platform.plagiarism.PlagiarismCheckService;
import com.hackathon.platform.plagiarism.PlagiarismProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class PlagiarismJobConsumerTest {

  @Mock private StringRedisTemplate redis;
  @Mock private PlagiarismCheckService checkService;
  @Mock private StreamOperations<String, Object, Object> streamOps;

  private PlagiarismJobConsumer consumer;

  @Test
  void onMessage_acksAfterSuccessfulRun() {

    PlagiarismProperties props = new PlagiarismProperties();
    props.getQueue().setConsumerKey("plagiarism-workers");

    consumer = new PlagiarismJobConsumer(redis, props, checkService);
    when(redis.opsForStream()).thenReturn((StreamOperations) streamOps);

    MapRecord<String, String, String> record =
      MapRecord.create("plagiarism:jobs", Map.of("runId", "7")).withId(RecordId.of("1-1"));

    consumer.onMessage(record);

    verify(checkService).execute(7L);
    verify(streamOps).acknowledge(eq(props.getQueue().getConsumerKey()), eq(record));


  }

}
