package com.hackathon.platform.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Azure Blob Storage.
 */
@Configuration
@Getter
public class AzureBlobConfig {

  @Value("${azure.storage.connection-string}")
  private String connectionString;

  @Value("${azure.storage.containers.event-resources}")
  private String eventResourcesContainer;

  @Value("${azure.storage.containers.submissions}")
  private String submissionsContainer;

  @Value("${azure.storage.containers.scoring-logs}")
  private String scoringLogsContainer;

  @Value("${azure.storage.sas-expiry-minutes:60}")
  private int sasExpiryMinutes;

 
}