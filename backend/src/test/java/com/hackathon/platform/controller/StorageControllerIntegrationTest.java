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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class StorageControllerIntegrationTest {
  @Autowired private MockMvc mockMvc;

  @MockBean private StorageService storageService;
  @MockBean private AzureBlobConfig config;

  private String eventId;
  private String teamId;
  private String submissionId;
  private String levelId;

  private String blobUrl;
  private String presignedUrl;

  private MockMultipartFile textFile;
  private MockMultipartFile solverFile;
  private MockMultipartFile imageFile;
  private MockMultipartFile zipFile;

  @BeforeEach
  void setUp() {
    eventId = "11111111-1111-1111-1111-111111111111";
    teamId = "22222222-2222-2222-2222-222222222222";
    submissionId = "33333333-3333-3333-3333-333333333333";
    levelId = "1";

    blobUrl = "https://hackathonplatform.blob.core.windows.net/test";
    presignedUrl = "https://hackathonplatform.blob.core.windows.net/test?sv=...";

    textFile = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
    solverFile = new MockMultipartFile("file", "solver.py", "text/plain", "solver code".getBytes());
    imageFile = new MockMultipartFile("file", "logo.png", "image/png", "imagedata".getBytes());
    zipFile = new MockMultipartFile("file", "source.zip", "application/zip", "zipdata".getBytes());

    when(config.getEventResourcesContainer()).thenReturn("event-resource");
    when(config.getSubmissionsContainer()).thenReturn("submissions");
    when(config.getScoringLogsContainer()).thenReturn("scoring-logs");
    when(config.getSasExpiryMinutes()).thenReturn(60);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(blobUrl);
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
        .thenReturn(presignedUrl);
  }

  @Test
  void uploadLevelFile_returns200WithStorageKeyAndBlobUrl() throws Exception {
    mockMvc
        .perform(
            multipart("/api/storage//events/{eventId}/levels/{levelId}/files", eventId, levelId)
                .file(textFile))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(blobUrl));
  }

  @Test
  void uploadSolverFile_returns200WithStorageKeyBlobUrlAndVersion() throws Exception {
    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/solver", eventId)
                .file(solverFile)
                .param("version", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(blobUrl))
        .andExpect(jsonPath("$.version").value("1"));
  }

  @Test
  void uploadBrandingAsset_returns200WithStorageKey() throws Exception {
    mockMvc
        .perform(multipart("/api/storage/events/{eventId}/branding", eventId).file(imageFile))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(blobUrl));
  }

  @Test
  void uploadSubmissionOutput_returns200WithStorageKey() throws Exception {
    mockMvc
        .perform(
            multipart(
                    "/api/storage/events/{eventId}/teams/{teamId}/submissions/{submissionId}/output",
                    eventId,
                    teamId,
                    submissionId)
                .file(textFile))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(blobUrl));
  }

  @Test
  void uploadSourceArchive_returns200WithStorageKey() throws Exception {
    mockMvc
        .perform(
            multipart(
                    "/api/storage/events/{eventId}/teams/{teamId}/submissions/{submissionId}/source",
                    eventId,
                    teamId,
                    submissionId)
                .file(zipFile))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(blobUrl));
  }

  @Test
  void getSubmissionOutputUrl_returns200WithPresignedUrl() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/storage/events/{eventId}/teams/{teamId}/submissions/{submissionId}/output/{filename}",
                eventId,
                teamId,
                submissionId,
                "output.txt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(presignedUrl));
  }

  @Test
  void getSourceArchiveUrl_returns200WithPresignedUrl() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/storage/events/{eventId}/teams/{teamId}/submissions/{submissionId}/source/{filename}",
                eventId,
                teamId,
                submissionId,
                "source.zip"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(presignedUrl));
  }

  @Test
  void getScoringLogUrl_returns200WithPresignedUrl() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/storage/events/{eventId}/submissions/{submissionId}/logs/{filename}",
                eventId,
                submissionId,
                "log.txt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(presignedUrl));
  }

  @Test
  void getLevelFileUrl_returns200WithPresignedUrl() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/storage/events/{eventId}/levels/{levelId}/files/{filename}",
                eventId,
                levelId,
                "test.txt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(presignedUrl));
  }
}
