package com.hackathon.platform.plagiarism.queue;

import com.hackathon.platform.plagiarism.PlagiarismCheckService;
import com.hackathon.platform.plagiarism.PlagiarismProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlagiarismJobConsumer
    implements StreamListener<String, MapRecord<String, String, String>> {}