package com.hackathon.platform.service;
 
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.storage.StorageException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
 
/**
 * Azure Blob Storage implementation of {@link StorageService}.
 * All binary assets (level files, submissions, solver versions, logs) are stored here.
 * Storage keys follow the conventions defined in {@link com.hackathon.platform.storage.BlobPath}.
 */
@Service
@RequiredArgsConstructor
public class AzureBlobStorageService implements StorageService {

  private static final Logger LOG = LoggerFactory.getLogger(AzureBlobStorageService.class);
 
  private final BlobServiceClient blobServiceClient;
  private final AzureBlobConfig config;
 
  /**
   * {@inheritDoc}
   */
  @Override
  public String upload(String containerName, String storageKey, MultipartFile file) {
    try {
      BlobClient blobClient = getBlobClient(containerName, storageKey);
      BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(file.getContentType());
      blobClient.upload(file.getInputStream(), file.getSize(), true);
      blobClient.setHttpHeaders(headers);
      LOG.info("Uploaded blob: container={} storageKey={} size={}",
          containerName, storageKey, file.getSize());
      return blobClient.getBlobUrl();
    } catch (IOException e) {
      LOG.error("Failed to upload blob: container={} storageKey={}", containerName, storageKey, e);
      throw new StorageException("Failed to upload file: " + storageKey, e);
    }
  }

}