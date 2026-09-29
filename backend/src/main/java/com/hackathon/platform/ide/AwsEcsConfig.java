package com.hackathon.platform.ide;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecs.EcsClient;

@Configuration
public class AwsEcsConfig {
  @Bean
  EcsClient ecsClient() {
    return EcsClient.builder()
        .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "af-south-1")))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }
}
