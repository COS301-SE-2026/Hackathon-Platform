package com.hackathon.platform.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for {@link StorageController} using MockMvc.
 * Security filters are disabled via addFilters=false so tests
 * focus on controller logic only.
 */
@WebMvcTest(StorageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StorageControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private StorageService storageService;
  @MockBean private AzureBlobConfig config;

  private static final String EVENT_ID = "11111111-1111-1111-1111-111111111111";
  private static final String TEAM_ID = "22222222-2222-2222-2222-222222222222";
  private static final String SUBMISSION_ID = "33333333-3333-3333-3333-333333333333";
  private static final String LEVEL_ID = "1";
  private static final String BLOB_URL =
      "https://hackathonplatform.blob.core.windows.net/test";
  private static final String PRESIGNED_URL =
      "https://hackathonplatform.blob.core.windows.net/test?sv=...";
  private static final String CONTAINER = "event-resources";



  @Test
  void uploadLevelFile_returns200WithStorageKeyAndBlobUrl() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(config.getSasExpiryMinutes()).thenReturn(60);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    MockMultipartFile file =
        new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

    mockMvc
        .perform(
            multipart(
                    "/api/storage/events/{eventId}/levels/{levelId}/files", EVENT_ID, LEVEL_ID)
                .file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(BLOB_URL));


  }

  @Test
  void uploadLevelFile_returns400WhenNoFileProvided() throws Exception {
    mockMvc
        .perform(
            multipart(
                "/api/storage/events/{eventId}/levels/{levelId}/files", EVENT_ID, LEVEL_ID))
        .andExpect(status().isBadRequest());

  }


  @Test
  void getLevelFileUrl_returns200WithPresignedUrl() throws Exception {

    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(config.getSasExpiryMinutes()).thenReturn(60);
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
        .thenReturn(PRESIGNED_URL);

    mockMvc
        .perform(
            get(
                "/api/storage/events/{eventId}/levels/{levelId}/files/{filename}",
                EVENT_ID,
                LEVEL_ID,
                "test.txt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL));
  }

  @Test
  void uploadSolver_returns200WithStorageKeyAndVersion() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    MockMultipartFile file =
        new MockMultipartFile("file", "solver.py", "text/plain", "solver code".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/solver", EVENT_ID)
                .file(file)
                .param("version", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.version").value("1"));

  }

  @Test
  void uploadBrandingAsset_returns200WithStorageKey() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    MockMultipartFile file =
        new MockMultipartFile("file", "logo.png", "image/png", "imagedata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/branding", EVENT_ID).file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(BLOB_URL));

  }


  @Test
  void uploadSubmissionOutput_returns200WithStorageKey() throws Exception {
    when(config.getSubmissionsContainer()).thenReturn("submissions");
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    MockMultipartFile file =
        new MockMultipartFile("file", "output.txt", "text/plain", "output data".getBytes());

    mockMvc
        .perform(
            multipart(
                    "/api/storage/events/{eventId}/teams/{teamId}/submissions/{submissionId}/output",
                    EVENT_ID,
                    TEAM_ID,
                    SUBMISSION_ID)
                .file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(BLOB_URL));

  }

  @Test
  void uploadSourceArchive_returns200WithStorageKey() throws Exception {
    when(config.getSubmissionsContainer()).thenReturn("submissions");
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "source.zip", "application/zip", "zipdata".getBytes());

    mockMvc
        .perform(
            multipart(
                    "/api/storage/events/{eventId}/teams/{teamId}/submissions/{submissionId}/source",
                    EVENT_ID,
                    TEAM_ID,
                    SUBMISSION_ID)
                .file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(BLOB_URL));
  }

  @Test
  void getSubmissionOutputUrl_returns200WithPresignedUrl() throws Exception {
    when(config.getSubmissionsContainer()).thenReturn("submissions");
    when(config.getSasExpiryMinutes()).thenReturn(60);
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
        .thenReturn(PRESIGNED_URL);

    mockMvc
        .perform(
            get(
                "/api/storage/events/{eventId}/teams/{teamId}/submissions/{submissionId}/output/{filename}",
                EVENT_ID,
                TEAM_ID,
                SUBMISSION_ID,
                "output.txt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL));



  }

  @Test
  void getSourceArchiveUrl_returns200WithPresignedUrl() throws Exception {
    when(config.getSubmissionsContainer()).thenReturn("submissions");
    when(config.getSasExpiryMinutes()).thenReturn(60);
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
        .thenReturn(PRESIGNED_URL);

    mockMvc
        .perform(
            get(
                "/api/storage/events/{eventId}/teams/{teamId}/submissions/{submissionId}/source/{filename}",
                EVENT_ID,
                TEAM_ID,
                SUBMISSION_ID,
                "source.zip"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL));
  }


  @Test
  void getScoringLogUrl_returns200WithPresignedUrl() throws Exception {
    when(config.getScoringLogsContainer()).thenReturn("scoring-logs");
    when(config.getSasExpiryMinutes()).thenReturn(60);
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
        .thenReturn(PRESIGNED_URL);

    mockMvc
        .perform(
            get(
                "/api/storage/events/{eventId}/submissions/{submissionId}/logs/{filename}",
                EVENT_ID,
                SUBMISSION_ID,
                "log.txt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL));
  }
  
}