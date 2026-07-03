package com.hackathon.platform.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.model.SolverVersion;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.UserRepository;
import com.hackathon.platform.service.FileMetadataService;
import com.hackathon.platform.service.StorageService;
import com.hackathon.platform.shared.security.JwtAuthFilter;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for {@link StorageController} using MockMvc. Security filters are disabled via
 * addFilters=false so tests focus on controller logic only.
 */
@WebMvcTest(StorageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StorageControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private StorageService storageService;
  @MockBean private AzureBlobConfig config;
  @MockBean private FileMetadataService fileMetadataService;
  @MockBean private SolverVersionRepository solverVersionRepository;
  @MockBean private JwtAuthFilter jwtAuthFilter;
  @MockBean private UserRepository userRepository;
  @MockBean private PasswordEncoder passwordEncoder;
  @MockBean private AuthenticationProvider authenticationProvider;

  private static final String HACKATHON_ID = "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13";
  private static final String TEAM_ID = "d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14";
  private static final String UPLOADED_BY = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11";
  private static final String LEVEL_ID = "1";
  private static final String SUBMISSION_ID = "1";
  private static final String BLOB_URL = "https://hackathonplatform.blob.core.windows.net/test";
  private static final String PRESIGNED_URL =
      "https://hackathonplatform.blob.core.windows.net/test?sv=...";
  private static final String CONTAINER = "event-resources";

  @Test
  void uploadLevelFile_returns200WithStorageKeyAndBlobUrl() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    LevelFile saved = new LevelFile(1L, "test.txt", "hackathons/.../test.txt", "TXT");
    saved.setId(1L);
    when(fileMetadataService.saveLevelFile(
            any(), anyString(), anyString(), anyString(), any(), any()))
        .thenReturn(saved);

    MockMultipartFile file =
        new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

    mockMvc
        .perform(
            multipart(
                    "/api/storage/hackathons/{hackathonId}/levels/{levelId}/files",
                    HACKATHON_ID,
                    LEVEL_ID)
                .file(file)
                .param("fileType", "TXT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(BLOB_URL))
        .andExpect(jsonPath("$.id").value("1"));
  }

  @Test
  void uploadLevelFile_returnsErrorWhenNoFileProvided() throws Exception {
    mockMvc
        .perform(
            multipart(
                    "/api/storage/hackathons/{hackathonId}/levels/{levelId}/files",
                    HACKATHON_ID,
                    LEVEL_ID)
                .param("fileType", "TXT"))
        .andExpect(status().is5xxServerError());
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
                "/api/storage/hackathons/{hackathonId}/levels/{levelId}/files/{filename}",
                HACKATHON_ID,
                LEVEL_ID,
                "test.txt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL));
  }

  @Test
  void uploadSolver_returns200WithStorageKeyAndVersion() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);
    when(solverVersionRepository.findByHackathonId(any())).thenReturn(Collections.emptyList());

    SolverVersion saved =
        new SolverVersion(
            UUID.fromString(HACKATHON_ID),
            UUID.fromString(UPLOADED_BY),
            "hackathons/.../solver.py");
    saved.setId(1L);
    when(fileMetadataService.saveSolverVersion(
            any(), any(), anyString(), any(), anyString(), any()))
        .thenReturn(saved);

    MockMultipartFile file =
        new MockMultipartFile("file", "solver.py", "text/plain", "solver code".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/hackathons/{hackathonId}/solver", HACKATHON_ID)
                .file(file)
                .param("version", "1")
                .param("uploadedBy", UPLOADED_BY)
                .param("notes", "Initial version"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.version").value("1"))
        .andExpect(jsonPath("$.solverVersionId").value("1"));
  }

  @Test
  void uploadBrandingAsset_returns200WithStorageKey() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    MockMultipartFile file =
        new MockMultipartFile("file", "logo.png", "image/png", "imagedata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/hackathons/{hackathonId}/branding", HACKATHON_ID).file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(BLOB_URL));
  }

  @Test
  void uploadSubmission_returns200WithBothStorageKeysAndSubmissionId() throws Exception {
    when(config.getSubmissionsContainer()).thenReturn("submissions");
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    Submission saved =
        new Submission(
            UUID.fromString(TEAM_ID),
            1L,
            1L,
            "submissions/.../levels/1/.../6/source/archive.zip",
            "submissions/.../levels/1/.../6/output/output.txt");
    saved.setId(6L);
    when(fileMetadataService.saveSubmission(
            anyString(),
            any(),
            any(),
            any(),
            anyString(),
            any(),
            anyString(),
            anyString(),
            any(),
            anyString()))
        .thenReturn(saved);

    MockMultipartFile outputFile =
        new MockMultipartFile("outputFile", "output.txt", "text/plain", "output data".getBytes());
    MockMultipartFile sourceFile =
        new MockMultipartFile("sourceFile", "archive.zip", "application/zip", "zipdata".getBytes());

    mockMvc
        .perform(
            multipart(
                    "/api/storage/hackathons/{hackathonId}/teams/{teamId}/submissions",
                    HACKATHON_ID,
                    TEAM_ID)
                .file(outputFile)
                .file(sourceFile)
                .param("levelId", "1")
                .param("solverVersionId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submissionId").value("6"))
        .andExpect(jsonPath("$.outputStorageKey").exists())
        .andExpect(jsonPath("$.sourceStorageKey").exists())
        .andExpect(jsonPath("$.status").value("QUEUED"));
  }

  @Test
  void uploadSubmission_returnsErrorWhenOutputFileMissing() throws Exception {
    MockMultipartFile sourceFile =
        new MockMultipartFile("sourceFile", "archive.zip", "application/zip", "zipdata".getBytes());

    mockMvc
        .perform(
            multipart(
                    "/api/storage/hackathons/{hackathonId}/teams/{teamId}/submissions",
                    HACKATHON_ID,
                    TEAM_ID)
                .file(sourceFile)
                .param("levelId", "1")
                .param("solverVersionId", "1"))
        .andExpect(status().is5xxServerError());
  }

  @Test
  void uploadSubmission_returnsErrorWhenLevelIdMissing() throws Exception {
    MockMultipartFile outputFile =
        new MockMultipartFile("outputFile", "output.txt", "text/plain", "output data".getBytes());
    MockMultipartFile sourceFile =
        new MockMultipartFile("sourceFile", "archive.zip", "application/zip", "zipdata".getBytes());

    mockMvc
        .perform(
            multipart(
                    "/api/storage/hackathons/{hackathonId}/teams/{teamId}/submissions",
                    HACKATHON_ID,
                    TEAM_ID)
                .file(outputFile)
                .file(sourceFile)
                .param("solverVersionId", "1"))
        .andExpect(status().is5xxServerError());
  }

  @Test
  void getSubmissionOutputUrl_returns200WithPresignedUrl() throws Exception {
    when(config.getSubmissionsContainer()).thenReturn("submissions");
    when(config.getSasExpiryMinutes()).thenReturn(60);
    when(fileMetadataService.getSubmissionOutputStorageKey(anyLong()))
        .thenReturn("submissions/.../levels/1/.../1/output/output.txt");
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
        .thenReturn(PRESIGNED_URL);

    mockMvc
        .perform(
            get(
                "/api/storage/hackathons/{hackathonId}/teams/{teamId}/submissions/{submissionId}/output/{filename}",
                HACKATHON_ID,
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
    when(fileMetadataService.getSubmissionSourceStorageKey(anyLong()))
        .thenReturn("submissions/.../levels/1/.../1/source/archive.zip");
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
        .thenReturn(PRESIGNED_URL);

    mockMvc
        .perform(
            get(
                "/api/storage/hackathons/{hackathonId}/teams/{teamId}/submissions/{submissionId}/source/{filename}",
                HACKATHON_ID,
                TEAM_ID,
                SUBMISSION_ID,
                "archive.zip"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL));
  }

  @Test
  void getSubmissionOutputUrl_returns5xxWhenSubmissionIdNotNumeric() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/storage/hackathons/{hackathonId}/teams/{teamId}/submissions/{submissionId}/output/{filename}",
                HACKATHON_ID,
                TEAM_ID,
                "not-a-number",
                "output.txt"))
        .andExpect(status().is5xxServerError());
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
                "/api/storage/hackathons/{hackathonId}/teams/{teamId}/levels/{levelId}",
                HACKATHON_ID,
                TEAM_ID,
                LEVEL_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL));
  }
}