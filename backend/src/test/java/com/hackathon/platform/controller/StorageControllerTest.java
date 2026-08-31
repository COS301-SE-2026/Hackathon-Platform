package com.hackathon.platform.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static
org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Level;
import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.SolverVersion;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.TeamMember;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.LevelRepository;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.scoring.queue.ScoringJobProducer;
import com.hackathon.platform.service.EventService;
import com.hackathon.platform.service.FileMetadataService;
import com.hackathon.platform.service.HackathonService;
import com.hackathon.platform.service.StorageService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class StorageControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private StorageService storageService;

  @MockBean private AzureBlobConfig config;

  @MockBean private FileMetadataService fileMetadataService;

  @MockBean private SolverVersionRepository solverVersionRepository;

  @MockBean private EventRepository eventRepository;

  @MockBean private EventService eventService;

  @MockBean private ScoringJobProducer producer;

  @MockBean private HackathonService hackathonService;
  @MockBean private EventRegistrationRepository eventRegRepo;
  @MockBean private TeamRepository teamRepo;
  @MockBean private TeamMemberRepository teamMemberRepo;
  @MockBean private LevelRepository levelRepo;
  @MockBean private SubmissionRepository subRepo;

  private static final String EVENT_ID = "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13";
  private static final String HACKATHON_ID = "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13";
  private static final String TEAM_ID = "d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14";
  private static final String UPLOADED_BY = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11";
  private static final Long LEVEL_ID = 1L;

  private static final Long SUBMISSION_ID = 100L;
  private static final String BLOB_URL = "https://hackathonplatform.blob.core.windows.net/test";
  private static final String PRESIGNED_URL =
      "https://hackathonplatform.blob.core.windows.net/test?sv=...";
  private static final String CONTAINER = "event-resources";
  private static final String SUBMISSIONS_CONTAINER = "submissions";

  private UsernamePasswordAuthenticationToken adminAuth;
  private UsernamePasswordAuthenticationToken participantAuth;

  @BeforeEach
  void setUp() {
    // Create admin user
    User adminUser =
        User.builder()
            .userId(UUID.fromString(UPLOADED_BY))
            .firstName("Admin")
            .lastName("User")
            .email("admin@test.com")
            .passwordHash("hash")
            .status("ACTIVE")
            .role(Role.builder().roleId(1).name("ADMIN").build())
            .build();

    // Create participant user
    User participantUser =
        User.builder()
            .userId(UUID.fromString(TEAM_ID))
            .firstName("Participant")
            .lastName("User")
            .email("participant@test.com")
            .passwordHash("hash")
            .status("ACTIVE")
            .role(Role.builder().roleId(2).name("PARTICIPANT").build())
            .build();

    // Create authentication tokens
    adminAuth =
        new UsernamePasswordAuthenticationToken(
            adminUser, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    participantAuth =
        new UsernamePasswordAuthenticationToken(
            participantUser, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));

    Event activeEvent = new Event();
    activeEvent.setStatus("ACTIVE");
    activeEvent.setHackathon(UUID.fromString(HACKATHON_ID));
    when(eventRepository.findByHackathon(any())).thenReturn(List.of(activeEvent));
    when(eventService.getEventById(any())).thenReturn(activeEvent);

    Team team = new Team();
    team.setEventId(UUID.fromString(EVENT_ID));
    when(teamRepo.findById(any())).thenReturn(Optional.of(team));

    TeamMember tm = new TeamMember();
    tm.setTeamId(UUID.fromString(TEAM_ID));
    when(teamMemberRepo.findByUserIdAndStatusAndEventId(any(), anyString(), any()))
        .thenReturn(List.of(tm));

    Level lvl = new Level();
    lvl.setHackathonId(UUID.fromString(HACKATHON_ID));
    when(levelRepo.findById(anyShort())).thenReturn(Optional.of(lvl));

    Submission submission = new Submission();
    submission.setEventId(UUID.fromString(EVENT_ID));
    submission.setTeamId(UUID.fromString(TEAM_ID));
    when(subRepo.findById(any())).thenReturn(Optional.of(submission));
  }

  @Test
  void uploadLevelFile_returns200WithStorageKeyAndBlobUrl() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    LevelFile saved = new LevelFile(LEVEL_ID, "test.txt", "hackathons/.../test.txt", "TXT");
    saved.setId(1L);
    when(fileMetadataService.saveLevelFile(
            anyShort(), anyString(), anyString(), anyString(), anyLong(), anyString()))
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
                .param("fileType", "TXT")
                .with(authentication(adminAuth)))
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
                .param("fileType", "TXT")
                .with(authentication(adminAuth)))
        .andExpect(status().is5xxServerError());
  }

  @Test
  void getLevelFileUrl_returns200WithPresignedUrl() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(config.getSasExpiryMinutes()).thenReturn(60);
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt(), anyString()))
        .thenReturn(PRESIGNED_URL);

    mockMvc
        .perform(
            get(
                    "/api/storage/hackathons/{hackathonId}/levels/{levelId}/files/{filename}",
                    HACKATHON_ID,
                    LEVEL_ID,
                    "test.txt")
                .with(authentication(participantAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL));
  }

  @Test
  void listLevelFiles_returns200WithFileList() throws Exception {
    List<LevelFile> files =
        Collections.singletonList(
            new LevelFile(LEVEL_ID, "test.txt", "hackathons/.../test.txt", "TXT"));
    when(fileMetadataService.listLevelFiles(any())).thenReturn(files);

    mockMvc
        .perform(
            get(
                    "/api/storage/hackathons/{hackathonId}/levels/{levelId}/files",
                    HACKATHON_ID,
                    LEVEL_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].fileName").value("test.txt"));
  }

  @Test
  void deleteLevelFile_returns204WhenSuccessful() throws Exception {
    LevelFile file = new LevelFile(LEVEL_ID, "test.txt", "hackathons/.../test.txt", "TXT");
    file.setId(1L);
    when(fileMetadataService.getLevelFile(any())).thenReturn(file);

    mockMvc
        .perform(
            delete(
                    "/api/storage/hackathons/{hackathonId}/levels/{levelId}/files/{fileId}",
                    HACKATHON_ID,
                    LEVEL_ID,
                    1L)
                .with(authentication(adminAuth)))
        .andExpect(status().isNoContent());
  }

  @Test
  void uploadSolver_returns200WithStorageKeyAndVersion() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);
    when(solverVersionRepository.findFirstByHackathonIdOrderByVersionNumberDesc(any()))
        .thenReturn(Optional.empty());

    SolverVersion saved =
        new SolverVersion(
            UUID.fromString(HACKATHON_ID),
            UUID.fromString(UPLOADED_BY),
            "hackathons/.../solver.py");
    saved.setId(1L);
    when(fileMetadataService.saveSolverVersion(
            any(), any(), anyString(), anyInt(), anyString(), anyLong(), anyString()))
        .thenReturn(saved);

    MockMultipartFile file =
        new MockMultipartFile("file", "solver.py", "text/plain", "solver code".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/hackathons/{hackathonId}/solver", HACKATHON_ID)
                .file(file)
                .param("notes", "Initial version")
                .with(authentication(adminAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.version").value("1"))
        .andExpect(jsonPath("$.solverVersionId").value("1"));
  }

  @Test
  void uploadEventBanner_returns200WithStorageKey() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    MockMultipartFile file =
        new MockMultipartFile("file", "banner.png", "image/png", "imagedata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/banner", EVENT_ID)
                .file(file)
                .with(authentication(adminAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(BLOB_URL));
  }

  @Test
  void uploadEventLogo_returns200WithStorageKey() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    MockMultipartFile file =
        new MockMultipartFile("file", "logo.png", "image/png", "imagedata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/logo", EVENT_ID)
                .file(file)
                .with(authentication(adminAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(BLOB_URL));
  }

  @Test
  void getEventBannerUrl_returnsPresignedUrl() throws Exception {
    Event event = new Event();
    event.setBannerStorageKey("events/" + EVENT_ID + "/branding/banner/banner.png");
    when(eventService.getEventById(UUID.fromString(EVENT_ID))).thenReturn(event);
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
        .thenReturn(BLOB_URL);

    mockMvc
        .perform(
            get("/api/storage/events/{eventId}/banner",
EVENT_ID).with(authentication(adminAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(BLOB_URL));
  }

  @Test
  void getEventBannerUrl_returns204WhenNoBannerUploaded() throws Exception {
    Event event = new Event();
    when(eventService.getEventById(UUID.fromString(EVENT_ID))).thenReturn(event);

    mockMvc
        .perform(
            get("/api/storage/events/{eventId}/banner",
EVENT_ID).with(authentication(adminAuth)))
        .andExpect(status().isNoContent());
  }

  @Test
  void getEventLogoUrl_returnsPresignedUrl() throws Exception {
    Event event = new Event();
    event.setLogoStorageKey("events/" + EVENT_ID + "/branding/logo/logo.png");
    when(eventService.getEventById(UUID.fromString(EVENT_ID))).thenReturn(event);
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
        .thenReturn(BLOB_URL);

    mockMvc
        .perform(
            get("/api/storage/events/{eventId}/logo", EVENT_ID).with(authentication(adminAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(BLOB_URL));
  }

  @Test
  void getEventLogoUrl_returns204WhenNoLogoUploaded() throws Exception {
    Event event = new Event();
    when(eventService.getEventById(UUID.fromString(EVENT_ID))).thenReturn(event);

    mockMvc
        .perform(
            get("/api/storage/events/{eventId}/logo", EVENT_ID).with(authentication(adminAuth)))
        .andExpect(status().isNoContent());
  }

  @Test
  void uploadEventBanner_returns500WhenContentTypeNotAnImage() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "banner.pdf", "application/pdf", "notanimage".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/banner", EVENT_ID)
                .file(file)
                .with(authentication(adminAuth)))
        .andExpect(status().is5xxServerError());
  }

  @Test
  void uploadEventBanner_returns500WhenFileMissing() throws Exception {
    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/banner", EVENT_ID)
                .with(authentication(adminAuth)))
        .andExpect(status().is5xxServerError());
  }

  @Test
  void uploadEventBanner_returns403WhenCallerIsNotAdmin() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "banner.png", "image/png", "imagedata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/banner", EVENT_ID)
                .file(file)
                .with(authentication(participantAuth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void uploadEventLogo_returns500WhenContentTypeNotAnImage() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "logo.pdf", "application/pdf", "notanimage".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/logo", EVENT_ID)
                .file(file)
                .with(authentication(adminAuth)))
        .andExpect(status().is5xxServerError());
  }

  @Test
  void uploadEventLogo_returns403WhenCallerIsNotAdmin() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "logo.png", "image/png", "imagedata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/logo", EVENT_ID)
                .file(file)
                .with(authentication(participantAuth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteEventBanner_returns204AndDeletesBlobWhenBannerExists() throws Exception {
    Event event = new Event();
    String storageKey = "events/" + EVENT_ID + "/branding/banner/banner.png";
    event.setBannerStorageKey(storageKey);
    when(eventService.getEventById(UUID.fromString(EVENT_ID))).thenReturn(event);
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);

    mockMvc
        .perform(
            delete("/api/storage/events/{eventId}/banner", EVENT_ID)
                .with(authentication(adminAuth)))
        .andExpect(status().isNoContent());

    verify(storageService, times(1)).delete(CONTAINER, storageKey);
    verify(eventService, times(1)).updateEventBanner(UUID.fromString(EVENT_ID), null);
  }

  @Test
  void deleteEventBanner_returns204WithoutDeletingWhenNoBannerExists() throws Exception {
    Event event = new Event();
    when(eventService.getEventById(UUID.fromString(EVENT_ID))).thenReturn(event);

    mockMvc
        .perform(
            delete("/api/storage/events/{eventId}/banner", EVENT_ID)
                .with(authentication(adminAuth)))
        .andExpect(status().isNoContent());

    verify(storageService, never()).delete(anyString(), anyString());
    verify(eventService, never()).updateEventBanner(any(), any());
  }

  @Test
  void deleteEventBanner_returns403WhenCallerIsNotAdmin() throws Exception {
    mockMvc
        .perform(
            delete("/api/storage/events/{eventId}/banner", EVENT_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteEventLogo_returns204AndDeletesBlobWhenLogoExists() throws Exception {
    Event event = new Event();
    String storageKey = "events/" + EVENT_ID + "/branding/logo/logo.png";
    event.setLogoStorageKey(storageKey);
    when(eventService.getEventById(UUID.fromString(EVENT_ID))).thenReturn(event);
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);

    mockMvc
        .perform(
            delete("/api/storage/events/{eventId}/logo",
EVENT_ID).with(authentication(adminAuth)))
        .andExpect(status().isNoContent());

    verify(storageService, times(1)).delete(CONTAINER, storageKey);
    verify(eventService, times(1)).updateEventLogo(UUID.fromString(EVENT_ID), null);
  }

  @Test
  void deleteEventLogo_returns204WithoutDeletingWhenNoLogoExists() throws Exception {
    Event event = new Event();
    when(eventService.getEventById(UUID.fromString(EVENT_ID))).thenReturn(event);

    mockMvc
        .perform(
            delete("/api/storage/events/{eventId}/logo",
EVENT_ID).with(authentication(adminAuth)))
        .andExpect(status().isNoContent());

    verify(storageService, never()).delete(anyString(), anyString());
    verify(eventService, never()).updateEventLogo(any(), any());
  }

  @Test
  void deleteEventLogo_returns403WhenCallerIsNotAdmin() throws Exception {
    mockMvc
        .perform(
            delete("/api/storage/events/{eventId}/logo", EVENT_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void uploadProblemStatement_returns200WithStorageKey() throws Exception {
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    MockMultipartFile file =
        new MockMultipartFile("file", "problem.pdf", "application/pdf", "pdfdata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/hackathons/{hackathonId}/problem-statement", HACKATHON_ID)
                .file(file)
                .with(authentication(adminAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storageKey").exists())
        .andExpect(jsonPath("$.blobUrl").value(BLOB_URL));
  }

  @Test
  void uploadProblemStatement_returnsErrorWhenNotPdf() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "problem.txt", "text/plain", "textdata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/hackathons/{hackathonId}/problem-statement", HACKATHON_ID)
                .file(file)
                .with(authentication(adminAuth)))
        .andExpect(status().is5xxServerError());
  }

  @Test
  void getProblemStatementUrl_returns200WithPresignedUrl() throws Exception {
    com.hackathon.platform.model.Hackathon hackathon = new
com.hackathon.platform.model.Hackathon();
    hackathon.setProblemStatementStorageKey("hackathons/.../problem.pdf");

    when(hackathonService.getHackathonById(any())).thenReturn(hackathon);
    when(config.getEventResourcesContainer()).thenReturn(CONTAINER);
    when(config.getSasExpiryMinutes()).thenReturn(60);
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt(), anyString()))
        .thenReturn(PRESIGNED_URL);

    mockMvc
        .perform(
            get("/api/storage/hackathons/{hackathonId}/problem-statement", HACKATHON_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL))
        .andExpect(jsonPath("$.storageKey").exists());
  }

  @Test
  void uploadSubmission_returns200WithBothStorageKeysAndSubmissionId() throws Exception {
    when(config.getSubmissionsContainer()).thenReturn(SUBMISSIONS_CONTAINER);
    when(storageService.upload(anyString(), anyString(), any())).thenReturn(BLOB_URL);

    UUID hackathonUuid = UUID.fromString(HACKATHON_ID);
    when(eventRepository.findHackathonIdByEventId(any())).thenReturn(Optional.of(hackathonUuid));

    SolverVersion solver =
        new SolverVersion(hackathonUuid, UUID.fromString(UPLOADED_BY), "storage/key");
    solver.setId(1L);
    when(solverVersionRepository.findByHackathonIdAndIsActiveTrue(any()))
        .thenReturn(Optional.of(solver));

    Submission saved =
        new Submission(
            UUID.fromString(TEAM_ID),
            LEVEL_ID.shortValue(),
            1L,
            "submissions/.../levels/1/.../6/source/archive.zip",
            "submissions/.../levels/1/.../6/output/output.txt");
    saved.setId(6L);

    when(fileMetadataService.saveSubmission(
            anyString(),
            any(),
            anyShort(),
            any(),
            anyString(),
            anyLong(),
            anyString(),
            anyString(),
            anyLong(),
            anyString()))
        .thenReturn(saved);

    when(producer.enqueue(any())).thenReturn("record-123");

    MockMultipartFile outputFile =
        new MockMultipartFile("outputFile", "output.txt", "text/plain", "output
data".getBytes());
    MockMultipartFile sourceFile =
        new MockMultipartFile("sourceFile", "archive.zip", "application/zip",
"zipdata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/teams/{teamId}/submissions", EVENT_ID,
TEAM_ID)
                .file(outputFile)
                .file(sourceFile)
                .param("levelId", "1")
                .with(authentication(participantAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submissionId").value("6"))
        .andExpect(jsonPath("$.outputStorageKey").exists())
        .andExpect(jsonPath("$.sourceStorageKey").exists())
        .andExpect(jsonPath("$.status").value("QUEUED"))
        .andExpect(jsonPath("$.scoringRecordId").value("record-123"));
  }

  @Test
  void uploadSubmission_returnsErrorWhenOutputFileMissing() throws Exception {
    MockMultipartFile sourceFile =
        new MockMultipartFile("sourceFile", "archive.zip", "application/zip",
"zipdata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/teams/{teamId}/submissions", EVENT_ID,
TEAM_ID)
                .file(sourceFile)
                .param("levelId", "1")
                .with(authentication(participantAuth)))
        .andExpect(status().is5xxServerError());
  }

  @Test
  void uploadSubmission_returnsErrorWhenLevelIdMissing() throws Exception {
    MockMultipartFile outputFile =
        new MockMultipartFile("outputFile", "output.txt", "text/plain", "output
data".getBytes());
    MockMultipartFile sourceFile =
        new MockMultipartFile("sourceFile", "archive.zip", "application/zip",
"zipdata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/teams/{teamId}/submissions", EVENT_ID,
TEAM_ID)
                .file(outputFile)
                .file(sourceFile)
                .with(authentication(participantAuth)))
        .andExpect(status().is5xxServerError());
  }

  @Test
  void getSubmissionOutputUrl_returns200WithPresignedUrl() throws Exception {
    when(config.getSubmissionsContainer()).thenReturn(SUBMISSIONS_CONTAINER);
    when(config.getSasExpiryMinutes()).thenReturn(60);
    when(fileMetadataService.getSubmissionOutputStorageKey(anyLong()))
        .thenReturn("submissions/.../levels/1/.../1/output/output.txt");
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
        .thenReturn(PRESIGNED_URL);

    mockMvc
        .perform(
            get(

"/api/storage/events/{eventId}/teams/{teamId}/submissions/{submissionId}/output/{filename}",
                    EVENT_ID,
                    TEAM_ID,
                    SUBMISSION_ID,
                    "output.txt")
                .with(authentication(participantAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL));
  }

  @Test
  void getSourceArchiveUrl_returns200WithPresignedUrl() throws Exception {
    when(config.getSubmissionsContainer()).thenReturn(SUBMISSIONS_CONTAINER);
    when(config.getSasExpiryMinutes()).thenReturn(60);
    when(fileMetadataService.getSubmissionSourceStorageKey(anyLong()))
        .thenReturn("submissions/.../levels/1/.../1/source/archive.zip");
    when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
        .thenReturn(PRESIGNED_URL);

    mockMvc
        .perform(
            get(

"/api/storage/events/{eventId}/teams/{teamId}/submissions/{submissionId}/source/{filename}",
                    EVENT_ID,
                    TEAM_ID,
                    SUBMISSION_ID,
                    "archive.zip")
                .with(authentication(adminAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL));
  }

  @Test
  void getSubmissionOutputUrl_returns5xxWhenSubmissionIdNotNumeric() throws Exception {
    mockMvc
        .perform(
            get(

"/api/storage/events/{eventId}/teams/{teamId}/submissions/{submissionId}/output/{filename}",
                    EVENT_ID,
                    TEAM_ID,
                    "not-a-number",
                    "output.txt")
                .with(authentication(participantAuth)))
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

"/api/storage/events/{eventId}/teams/{teamId}/levels/{levelId}/submissions/{submissionId}",
                    EVENT_ID,
                    TEAM_ID,
                    LEVEL_ID,
                    SUBMISSION_ID)
                .with(authentication(participantAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(PRESIGNED_URL));
  }

  @Test
  void uploadLevelFile_returns403WhenCallerIsNotAdmin() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

    mockMvc
        .perform(
            multipart(
                    "/api/storage/hackathons/{hackathonId}/levels/{levelId}/files",
                    HACKATHON_ID,
                    LEVEL_ID)
                .file(file)
                .param("fileType", "TXT")
                .with(authentication(participantAuth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void uploadSubmission_returns403WhenCallerIsAdminNotParticipant() throws Exception {
    MockMultipartFile outputFile =
        new MockMultipartFile("outputFile", "output.txt", "text/plain", "output
data".getBytes());
    MockMultipartFile sourceFile =
        new MockMultipartFile("sourceFile", "archive.zip", "application/zip",
"zipdata".getBytes());

    mockMvc
        .perform(
            multipart("/api/storage/events/{eventId}/teams/{teamId}/submissions", EVENT_ID,
TEAM_ID)
                .file(outputFile)
                .file(sourceFile)
                .param("levelId", "1")
                .with(authentication(adminAuth)))
        .andExpect(status().isForbidden());
  }
}
