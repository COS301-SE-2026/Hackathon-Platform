package com.hackathon.platform.plagiarism.ast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Thin HTTP client for the Python tree-sitter/CodeBERT microservice.
 */
@Component
public class AstServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AstServiceClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final PlagiarismAstProperties props;

    public AstServiceClient(PlagiarismAstProperties props) {
        
        this.props = props;
        this.httpClient =
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))

                //Pin to HTTP/1.1 explicitly due to upgrade preface corrupting framing on reused connection.
                .version(HttpClient.version.HTTP_1_1)
                .build();
            this.mapper =
                new ObjectMapper()
                //Kept as a defense fallback, do not rely on alone.

                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    

    }

    private record ParseRequestBody(@JsonProperty("file_name") String fileName, @JsonProperty("content") String content) {}

    
}