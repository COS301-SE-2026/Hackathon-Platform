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

  
}