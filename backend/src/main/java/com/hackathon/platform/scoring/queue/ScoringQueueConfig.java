package com.hackathon.platform.scoring.queue;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

@Configuration
@RequiredArgsConstructor
public class ScoringQueueConfig {
  private static final Logger logger = LoggerFactory.getLogger(ScoringQueueConfig.class);
  private final StringRedisTemplate redis;
  private final ScoringQueueProperties properties;
  private final ScoringJobConsumer consumer;

  @PostConstruct
  public void createConsumerGroup() {
    try {
      redis
          .opsForStream()
          .createGroup(
              properties.getStreamKey(), ReadOffset.from("0"), properties.getConsumerKey());
      logger.info(
          "Created consumer group {} on stream no. {}",
          properties.getConsumerKey(),
          properties.getStreamKey());
    } catch (Exception e) {
      if (isBusyGroupError(e)) {
        logger.info("Consumer group {} exists", properties.getConsumerKey());
      } else {
        throw e;
      }
    }
  }

  private boolean isBusyGroupError(Throwable e) {
    Throwable curr = e;
    while (curr != null) {
      if (curr.getMessage() != null & curr.getMessage().contains("BUSYGROUP")) {
        return true;
      }
      curr = curr.getCause();
    }
    return false;
  }

  @Bean(destroyMethod = "shutdown")
  public ExecutorService scoringStreamExecutor() {
    return Executors.newFixedThreadPool(properties.getConcurrency());
  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  public StreamMessageListenerContainer<String, MapRecord<String, String, String>>
      scoringStreamContainer(
          RedisConnectionFactory connection,
          @Qualifier("scoringStreamExecutor") ExecutorService executor) {
    StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
        StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ofMillis(properties.getPollTimeoutMs()))
            .executor(executor)
            .build();
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
        StreamMessageListenerContainer.create(connection, options);

    for (int i = 0; i < properties.getConcurrency(); i++) {
      String name = "worker-" + i;
      container.receive(
          Consumer.from(properties.getConsumerKey(), name),
          StreamOffset.create(properties.getStreamKey(), ReadOffset.lastConsumed()),
          consumer);
    }
    return container;
  }
}
