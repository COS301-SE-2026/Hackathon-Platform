package com.hackathon.platform.plagiarism.queue;

import com.hackathon.platform.plagiarism.PlagiarismProperties;
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

/**
 * Never contend with the per-submission scoring pipeline.
 */
@Configuration
@RequiredArgsConstructor
public class PlagiarismQueueConfig {
  private static final Logger logger = LoggerFactory.getLogger(PlagiarismQueueConfig.class);
  private final StringRedisTemplate redis;
  private final PlagiarismProperties properties;
  private final PlagiarismJobConsumer consumer;

  @PostConstruct
  public void createConsumerGroup() {
    try {
        redis
            .opsForStream()
            .createGroup(
                properties.getQueue().getStreamKey(),
                ReadOffset.from("0"),
                properties.getQueue().getConsumerKey()
            );
        logger.info("Created consumer group {} on stream {}",
            properties.getQueue().getConsumerKey(), properties.getQueue().getStreamKey());
        
    } catch (Exception e) {

        if(isBusyGroupError(e)) {
            logger.info("Consumer group {} exists", properties.getQueue().getConsumerKey());

        } else {
            throw e;

        }
    }
  }

  private boolean isBusyGroupError(Throwable e) {
    Throwable curr = e;

    while(curr != null) {
        if(curr.getMessage() != null && curr.getMessage().contains("BUSYGROUP")) {
            return true;
        }
        curr = curr.getCause();

    }
    return false;
  }

  @Bean(destroyMethod = "shutdown")
  public ExecutorService plagiarismStreamExecutor() {
    return Executors.newFixedThreadPool(properties.getQueue().getConcurrency());

  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  public StreamMessageListenerContainer<String, MapRecord<String, String, String>>
      plagiarismStreamContainer(
        RedisConnectionFactory connection,
        @Qualifier("plagiarismStreamExecutor") ExecutorService executor
      ) {
    StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
        StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ofMillis(properties.getQueue().getPollTimeoutMs()))
            .executor(executor)
            .errorHandler(t -> logger.error("Plagiarism stream listener error", t))
            .build();

    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
        StreamMessageListenerContainer.create(connection, options);
    
    for(int i = 0; i < properties.getQueue().getConcurrency(); i++) {
        String name = "worker-" + i;
        container.receive(
            Consumer.from(properties.getQueue().getConsumerKey(), name),
            StreamOffset.create(properties.getQueue().getStreamKey(), ReadOffset.lastConsumed()),
            consumer 
        );
    }
    return container;
    
    }
}