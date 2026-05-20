package com.hackathon.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.storage.StorageException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

/** Unit tests for {@link AzureBlobStorageService}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AzureBlobStorageServiceTest {

  @Mock private BlobServiceClient blobServiceClient;
  @Mock private BlobContainerClient containerClient;
  @Mock private BlobClient blobClient;
  @Mock private AzureBlobConfig config;

  @InjectMocks private AzureBlobStorageService storageService;

  private static final String CONTAINER = "event-resources";
  private static final String STORAGE_KEY = "events/123/levels/1/test.txt";
  private static final String BLOB_URL =
      "https://hackathonplatform.blob.core.windows.net/event-resources/events/123/levels/1/test.txt";


  @BeforeEach
  void setUp() {
    when(blobServiceClient.getBlobContainerClient(CONTAINER)).thenReturn(containerClient);
    when(containerClient.getBlobClient(STORAGE_KEY)).thenReturn(blobClient);
    when(blobClient.getBlobUrl()).thenReturn(BLOB_URL);
  }

  @Test
  void upload_successfullyUploadsFileAndReturnsBlobUrl() throws IOException {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.txt", "text/plain", "hello world".getBytes());

    String result = storageService.upload(CONTAINER, STORAGE_KEY, file);

    verify(blobClient).upload(any(InputStream.class), anyLong(), anyBoolean());
    assertEquals(BLOB_URL, result);

  }


  @Test
  void upload_throwsStorageExceptionOnIoError() {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes()) {
          @Override
          public InputStream getInputStream() throws IOException {
            throw new IOException("Simulated IO error");
          }
        };

    assertThrows(StorageException.class, () -> storageService.upload(CONTAINER, STORAGE_KEY, file));
  }


  @Test
  void uploadBytes_successfullyUploadsBytesAndReturnsBlobUrl() {
    byte[] data = "log content".getBytes();

    String result = storageService.uploadBytes(CONTAINER, STORAGE_KEY, data, "text/plain");

    verify(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), anyBoolean());
    assertEquals(BLOB_URL, result);
  }

  @Test
  void download_returnsInputStream() {
    BlobInputStream mockStream = mock(BlobInputStream.class);
    when(blobClient.openInputStream()).thenReturn(mockStream);

    InputStream result = storageService.download(CONTAINER, STORAGE_KEY);

    assertEquals(mockStream, result);
  }

  @Test
  void download_throwsStorageExceptionOnFailure() {
    when(blobClient.openInputStream()).thenThrow(new RuntimeException("Azure error"));

    assertThrows(
        StorageException.class, () -> storageService.download(CONTAINER, STORAGE_KEY));

  }

  @Test
  void delete_callsDeleteIfExists() {
    storageService.delete(CONTAINER, STORAGE_KEY);
    verify(blobClient).deleteIfExists();
  }

  @Test
  void delete_throwsStorageExceptionOnFailure() {
    doThrow(new RuntimeException("Azure error")).when(blobClient).deleteIfExists();

    assertThrows(
        StorageException.class, () -> storageService.delete(CONTAINER, STORAGE_KEY));
  }

  @Test
  void exists_returnsTrueWhenBlobExists() {
    when(blobClient.exists()).thenReturn(true);
    assertTrue(storageService.exists(CONTAINER, STORAGE_KEY));

  }
  

}