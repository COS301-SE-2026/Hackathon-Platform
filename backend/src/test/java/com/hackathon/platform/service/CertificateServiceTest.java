package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.certificate.CertificateGenerator;
import com.hackathon.platform.certificate.CertificateRecipient;
import com.hackathon.platform.certificate.CertificateRecipientResolver;
import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.dto.CertificateTemplateRequest;
import com.hackathon.platform.dto.CertificateVerificationResponse;
import com.hackathon.platform.model.CertificateGenerationRun;
import com.hackathon.platform.model.CertificateIssued;
import com.hackathon.platform.model.CertificateLayout;
import com.hackathon.platform.model.CertificateLayout.CertificateElement;
import com.hackathon.platform.model.CertificateTemplate;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Hackathon;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.CertificateGenerationRunRepository;
import com.hackathon.platform.repository.CertificateIssuedRepository;
import com.hackathon.platform.repository.CertificateTemplateRepository;
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.HackathonRepository;
import com.hackathon.platform.repository.TeamRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {
  private static final String CONTAINER = "event-resources";

  @Mock private EventRepository eventRepo;
  @Mock private HackathonRepository hackRepo;
  @Mock private EventRegistrationRepository eventRegRepo;
  @Mock private StorageService storageService;
  @Mock private AzureBlobConfig blobConfig;
  @Mock private CertificateTemplateRepository templateRepo;
  @Mock private CertificateGenerationRunRepository runRepo;
  @Mock private CertificateIssuedRepository issuedRepo;
  @Mock private CertificateRecipientResolver recipientResolver;
  @Mock private CertificateGenerator certificateGenerator;
  @Mock private TeamRepository teamRepository;

  private CertificateService service;
  private UUID eventId;
  private UUID templateId;
  private Event event;

  @BeforeEach
  void setUp() {
    service =
        new CertificateService(
            eventRepo,
            hackRepo,
            eventRegRepo,
            storageService,
            blobConfig,
            templateRepo,
            runRepo,
            issuedRepo,
            recipientResolver,
            certificateGenerator,
            teamRepository);
    lenient().when(blobConfig.getEventResourcesContainer()).thenReturn(CONTAINER);
    eventId = UUID.randomUUID();
    templateId = UUID.randomUUID();
    event = new Event();
    event.setEventId(eventId);
    event.setName("Spring Hack");
    event.setHackathon(UUID.randomUUID());
  }

  private static byte[] png() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", out);
    return out.toByteArray();
  }

  private static String textOf(byte[] pdf) throws IOException {
    try (PDDocument doc = Loader.loadPDF(pdf)) {
      return new PDFTextStripper().getText(doc);
    }
  }

  private User user() {
    return User.builder()
        .userId(UUID.randomUUID())
        .firstName("Charles")
        .lastName("Leclerc")
        .build();
  }

  private static CertificateElement el(String type, String imageKey) {
    CertificateElement e = new CertificateElement();
    e.setType(type);
    e.setImageStorageKey(imageKey);
    return e;
  }

  private CertificateTemplate template(String backgroundKey, CertificateElement... elements) {
    CertificateTemplate t = new CertificateTemplate();
    t.setTemplateId(templateId);
    t.setBackgroundStorageKey(backgroundKey);
    CertificateLayout layout = new CertificateLayout();
    layout.setElements(List.of(elements));
    t.setLayout(layout);
    return t;
  }

  private CertificateGenerationRun run(String scope, Integer topN) {
    CertificateGenerationRun r = new CertificateGenerationRun();
    r.setRunId(UUID.randomUUID());
    r.setEventId(eventId);
    r.setTemplateId(templateId);
    r.setScope(scope);
    r.setTopN(topN);
    return r;
  }

  private CertificateRecipient recipient(String name, int rank, String type) {
    return new CertificateRecipient(
        UUID.randomUUID(), UUID.randomUUID(), name, rank, type, Map.of("participantName", name));
  }

  private void stubRunLookup(CertificateGenerationRun run) {
    when(runRepo.findById(run.getRunId())).thenReturn(Optional.of(run));
    when(runRepo.save(any(CertificateGenerationRun.class))).then(returnsFirstArg());
  }

  // ---------- genCertificate ----------

  @Test
  void genCertificate_throws_whenEventMissing() {
    when(eventRepo.findById(eventId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.genCertificate(eventId, user()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Event not found");
  }

  @Test
  void genCertificate_throws_whenNotRegistered() {
    User u = user();
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegRepo.existsByEventIdAndUserId(eventId, u.getUserId())).thenReturn(false);
    assertThatThrownBy(() -> service.genCertificate(eventId, u))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("You are not registered for this event");
  }

  @Test
  void genCertificate_usesHackathonNameAndParticipantName() throws IOException {
    User u = user();
    Hackathon h = new Hackathon();
    h.setName("Mega Hackathon");
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegRepo.existsByEventIdAndUserId(eventId, u.getUserId())).thenReturn(true);
    when(hackRepo.findById(event.getHackathon())).thenReturn(Optional.of(h));

    byte[] pdf = service.genCertificate(eventId, u);

    String text = textOf(pdf);
    assertThat(text).contains("Mega Hackathon").contains("Charles Leclerc").contains("Spring Hack");
    verify(storageService, never()).download(anyString(), anyString());
  }

  @Test
  void genCertificate_fallsBackToEventName_whenHackathonMissing() throws IOException {
    User u = user();
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegRepo.existsByEventIdAndUserId(eventId, u.getUserId())).thenReturn(true);
    when(hackRepo.findById(event.getHackathon())).thenReturn(Optional.empty());

    assertThat(textOf(service.genCertificate(eventId, u))).contains("Spring Hack");
  }

  @Test
  void genCertificate_embedsLogo_whenAvailable() throws IOException {
    User u = user();
    event.setLogoStorageKey("logo.png");
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegRepo.existsByEventIdAndUserId(eventId, u.getUserId())).thenReturn(true);
    when(hackRepo.findById(event.getHackathon())).thenReturn(Optional.empty());
    when(storageService.download(CONTAINER, "logo.png"))
        .thenReturn(new ByteArrayInputStream(png()));

    byte[] pdf = service.genCertificate(eventId, u);

    assertThat(pdf).isNotEmpty();
    verify(storageService).download(CONTAINER, "logo.png");
    try (PDDocument doc = Loader.loadPDF(pdf)) {
      assertThat(doc.getPage(0).getResources().getXObjectNames()).hasSize(1);
    }
  }

  @Test
  void genCertificate_stillWorks_whenLogoDownloadFails() {
    User u = user();
    event.setLogoStorageKey("logo.png");
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegRepo.existsByEventIdAndUserId(eventId, u.getUserId())).thenReturn(true);
    when(hackRepo.findById(event.getHackathon())).thenReturn(Optional.empty());
    when(storageService.download(CONTAINER, "logo.png")).thenThrow(new RuntimeException("gone"));

    assertThat(service.genCertificate(eventId, u)).isNotEmpty();
  }

  @Test
  void genCertificate_skipsLogo_whenKeyBlank() {
    User u = user();
    event.setLogoStorageKey("   ");
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegRepo.existsByEventIdAndUserId(eventId, u.getUserId())).thenReturn(true);
    when(hackRepo.findById(event.getHackathon())).thenReturn(Optional.empty());

    assertThat(service.genCertificate(eventId, u)).isNotEmpty();
    verify(storageService, never()).download(anyString(), anyString());
  }

  // ---------- templates ----------

  @Test
  void createTemplate_copiesRequestFields_andSaves() {
    CertificateTemplateRequest req = new CertificateTemplateRequest();
    req.setName("Tpl");
    req.setEventId(eventId);
    req.setHackathonId(UUID.randomUUID());
    req.setLayout(new CertificateLayout());
    UUID creator = UUID.randomUUID();
    when(templateRepo.save(any(CertificateTemplate.class))).then(returnsFirstArg());

    CertificateTemplate saved = service.createTemplate(req, creator);

    assertThat(saved.getName()).isEqualTo("Tpl");
    assertThat(saved.getEventId()).isEqualTo(eventId);
    assertThat(saved.getHackathonId()).isEqualTo(req.getHackathonId());
    assertThat(saved.getLayout()).isSameAs(req.getLayout());
    assertThat(saved.getCreatedByUserId()).isEqualTo(creator);
  }

  @Test
  void updateTemplate_updatesNameLayoutAndTimestamp() {
    CertificateTemplate existing = template(null);
    existing.setName("Old");
    OffsetDateTime old = OffsetDateTime.now().minusDays(1);
    existing.setUpdatedAt(old);
    CertificateTemplateRequest req = new CertificateTemplateRequest();
    req.setName("New");
    req.setLayout(new CertificateLayout());
    when(templateRepo.findById(templateId)).thenReturn(Optional.of(existing));
    when(templateRepo.save(existing)).thenReturn(existing);

    CertificateTemplate result = service.updateTemplate(templateId, req);

    assertThat(result.getName()).isEqualTo("New");
    assertThat(result.getLayout()).isSameAs(req.getLayout());
    assertThat(result.getUpdatedAt()).isAfter(old);
  }

  @Test
  void updateTemplate_throws_whenMissing() {
    when(templateRepo.findById(templateId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.updateTemplate(templateId, new CertificateTemplateRequest()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Certificate template not found");
    verify(templateRepo, never()).save(any());
  }

  @Test
  void uploadBackground_uploadsSanitisedKey_andStoresIt() {
    CertificateTemplate t = template(null);
    MockMultipartFile file =
        new MockMultipartFile("file", "../bg.png", "image/png", new byte[] {1});
    when(templateRepo.findById(templateId)).thenReturn(Optional.of(t));

    String key = service.uploadBackground(templateId, file);

    assertThat(key).isEqualTo("certificates/templates/" + templateId + "/background/__bg.png");
    assertThat(t.getBackgroundStorageKey()).isEqualTo(key);
    verify(storageService).upload(CONTAINER, key, file);
    verify(templateRepo).save(t);
  }

  @Test
  void uploadBackground_throws_whenTemplateMissing() {
    when(templateRepo.findById(templateId)).thenReturn(Optional.empty());
    MockMultipartFile file = new MockMultipartFile("file", "bg.png", "image/png", new byte[] {1});
    assertThatThrownBy(() -> service.uploadBackground(templateId, file))
        .isInstanceOf(IllegalArgumentException.class);
    verify(storageService, never()).upload(anyString(), anyString(), any());
  }

  @Test
  void uploadTemplateAsset_uploadsAndReturnsKey() {
    when(templateRepo.findById(templateId)).thenReturn(Optional.of(template(null)));
    MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1});

    String key = service.uploadTemplateAsset(templateId, file);

    assertThat(key).isEqualTo("certificates/templates/" + templateId + "/assets/logo.png");
    verify(storageService).upload(CONTAINER, key, file);
    verify(templateRepo, never()).save(any());
  }

  @Test
  void uploadTemplateAsset_throws_whenTemplateMissing() {
    when(templateRepo.findById(templateId)).thenReturn(Optional.empty());
    MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1});
    assertThatThrownBy(() -> service.uploadTemplateAsset(templateId, file))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void resolveAssetUrl_delegatesToStorage() {
    when(storageService.generatePresignedUrl(CONTAINER, "k", 60)).thenReturn("http://k");
    assertThat(service.resolveAssetUrl("k")).isEqualTo("http://k");
  }

  @Test
  void resolveTemplateAssetUrls_onlyImageElementsWithKeys_deduplicated() {
    CertificateTemplate t =
        template(
            null,
            el("IMAGE", "a.png"),
            el("IMAGE", "a.png"),
            el("IMAGE", null),
            el("TEXT", "ignored.png"),
            el("IMAGE", "b.png"));
    when(storageService.generatePresignedUrl(CONTAINER, "a.png", 60)).thenReturn("http://a");
    when(storageService.generatePresignedUrl(CONTAINER, "b.png", 60)).thenReturn("http://b");

    Map<String, String> urls = service.resolveTemplateAssetUrls(t);

    assertThat(urls).containsOnlyKeys("a.png", "b.png");
    assertThat(urls).containsEntry("a.png", "http://a").containsEntry("b.png", "http://b");
    verify(storageService, times(1)).generatePresignedUrl(CONTAINER, "a.png", 60);
  }

  @Test
  void getTemplate_returnsTemplate_orThrows() {
    CertificateTemplate t = template(null);
    when(templateRepo.findById(templateId)).thenReturn(Optional.of(t));
    assertThat(service.getTemplate(templateId)).isSameAs(t);

    UUID other = UUID.randomUUID();
    when(templateRepo.findById(other)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getTemplate(other))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Certificate template not found");
  }

  @Test
  void getTemplatesForEvent_withoutHackathon_onlyEventTemplates() {
    CertificateTemplate a = template(null);
    when(templateRepo.findByEventId(eventId)).thenReturn(new ArrayList<>(List.of(a)));

    assertThat(service.getTemplatesForEvent(eventId, null)).containsExactly(a);
    verify(templateRepo, never()).findByHackathonIdAndEventIdIsNull(any());
  }

  @Test
  void getTemplatesForEvent_withHackathon_addsHackathonLevelTemplates() {
    UUID hackId = UUID.randomUUID();
    CertificateTemplate a = template(null);
    CertificateTemplate b = template(null);
    when(templateRepo.findByEventId(eventId)).thenReturn(new ArrayList<>(List.of(a)));
    when(templateRepo.findByHackathonIdAndEventIdIsNull(hackId)).thenReturn(List.of(b));

    assertThat(service.getTemplatesForEvent(eventId, hackId)).containsExactly(a, b);
  }

  @Test
  void deleteTemplate_delegatesToRepo() {
    service.deleteTemplate(templateId);
    verify(templateRepo).deleteById(templateId);
  }

  @Test
  void resolveBackgroundUrl_nullWhenNoBackground_urlWhenPresent() {
    assertThat(service.resolveBackgroundUrl(template(null))).isNull();

    when(storageService.generatePresignedUrl(CONTAINER, "bg.png", 60)).thenReturn("http://bg");
    assertThat(service.resolveBackgroundUrl(template("bg.png"))).isEqualTo("http://bg");
  }

  // ---------- startGeneration ----------

  @Test
  void startGeneration_throws_whenEventMissing() {
    when(eventRepo.existsById(eventId)).thenReturn(false);
    assertThatThrownBy(
            () -> service.startGeneration(eventId, templateId, "TOP_N", 3, UUID.randomUUID()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Event not found");
    verify(runRepo, never()).save(any());
  }

  @Test
  void startGeneration_throws_whenTemplateMissing() {
    when(eventRepo.existsById(eventId)).thenReturn(true);
    when(templateRepo.findById(templateId)).thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> service.startGeneration(eventId, templateId, null, null, UUID.randomUUID()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Certificate template not found");
  }

  @Test
  void startGeneration_defaultsScopeToAllParticipants() {
    UUID requester = UUID.randomUUID();
    when(eventRepo.existsById(eventId)).thenReturn(true);
    when(templateRepo.findById(templateId)).thenReturn(Optional.of(template(null)));
    when(runRepo.save(any(CertificateGenerationRun.class))).then(returnsFirstArg());

    CertificateGenerationRun run =
        service.startGeneration(eventId, templateId, null, null, requester);

    assertThat(run.getScope()).isEqualTo("ALL_PARTICIPANTS");
    assertThat(run.getStatus()).isEqualTo("PENDING");
    assertThat(run.getEventId()).isEqualTo(eventId);
    assertThat(run.getTemplateId()).isEqualTo(templateId);
    assertThat(run.getRequestedByUserId()).isEqualTo(requester);
  }

  @Test
  void startGeneration_keepsExplicitScopeAndTopN() {
    when(eventRepo.existsById(eventId)).thenReturn(true);
    when(templateRepo.findById(templateId)).thenReturn(Optional.of(template(null)));
    when(runRepo.save(any(CertificateGenerationRun.class))).then(returnsFirstArg());

    CertificateGenerationRun run =
        service.startGeneration(eventId, templateId, "TOP_N", 3, UUID.randomUUID());

    assertThat(run.getScope()).isEqualTo("TOP_N");
    assertThat(run.getTopN()).isEqualTo(3);
  }

  // ---------- runGeneration ----------

  @Test
  void runGeneration_doesNothing_whenRunMissing() {
    UUID runId = UUID.randomUUID();
    when(runRepo.findById(runId)).thenReturn(Optional.empty());
    service.runGeneration(runId);
    verify(runRepo, never()).save(any());
  }

  @Test
  void runGeneration_issuesCertificateForEveryRecipient() throws Exception {
    CertificateGenerationRun run = run("TOP_N", 3);
    CertificateTemplate t =
        template(
            "bg.png",
            el("IMAGE", "logo.png"),
            el("IMAGE", "logo.png"),
            el("IMAGE", null),
            el("TEXT", null));
    CertificateRecipient r1 = recipient("Charles Leclerc", 1, "WINNER");
    CertificateRecipient r2 = recipient("Harry Lewis", 4, "PARTICIPATION");
    byte[] pdf = {9, 9, 9};
    stubRunLookup(run);
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(templateRepo.findById(templateId)).thenReturn(Optional.of(t));
    when(storageService.download(CONTAINER, "bg.png"))
        .thenReturn(new ByteArrayInputStream(new byte[] {1}));
    when(storageService.download(CONTAINER, "logo.png"))
        .thenReturn(new ByteArrayInputStream(new byte[] {2}));
    when(recipientResolver.resolve(event, "TOP_N", 3)).thenReturn(List.of(r1, r2));
    when(certificateGenerator.generate(any(), any(), any(), any(), anyString(), anyString()))
        .thenReturn(pdf);
    when(issuedRepo.existsByVerificationCode(anyString())).thenReturn(true, false);

    service.runGeneration(run.getRunId());

    assertThat(run.getStatus()).isEqualTo("COMPLETED");
    assertThat(run.getTotalCount()).isEqualTo(2);
    assertThat(run.getCompletedCount()).isEqualTo(2);
    assertThat(run.getCompletedAt()).isNotNull();
    verify(storageService, times(1)).download(CONTAINER, "logo.png");

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
    verify(certificateGenerator, times(2))
        .generate(any(), any(), any(), any(), urlCaptor.capture(), typeCaptor.capture());
    assertThat(urlCaptor.getAllValues())
        .allSatisfy(u -> assertThat(u).startsWith("https://hackathonplatform.co.za/verify/"));
    assertThat(typeCaptor.getAllValues()).containsExactly("WINNER", "PARTICIPATION");

    ArgumentCaptor<CertificateIssued> issuedCaptor =
        ArgumentCaptor.forClass(CertificateIssued.class);
    verify(issuedRepo, times(2)).save(issuedCaptor.capture());
    CertificateIssued first = issuedCaptor.getAllValues().get(0);
    assertThat(first.getRunId()).isEqualTo(run.getRunId());
    assertThat(first.getTemplateId()).isEqualTo(templateId);
    assertThat(first.getEventId()).isEqualTo(eventId);
    assertThat(first.getUserId()).isEqualTo(r1.getUserId());
    assertThat(first.getTeamId()).isEqualTo(r1.getTeamId());
    assertThat(first.getCertificateType()).isEqualTo("WINNER");
    assertThat(first.getRecipientName()).isEqualTo("Charles Leclerc");
    assertThat(first.getRankAtIssue()).isEqualTo(1);
    assertThat(first.getVerificationCode()).hasSize(10).matches("[A-HJ-NP-Z2-9]{10}");
    assertThat(first.getStorageKey())
        .startsWith("certificates/events/" + eventId + "/runs/" + run.getRunId() + "/")
        .endsWith(".pdf");
    verify(storageService, times(2))
        .uploadBytes(
            eq(CONTAINER), startsWith("certificates/events/"), eq(pdf), eq("application/pdf"));
    // first code collided, so the generator asked again: 2 checks for r1 + 1 for r2
    verify(issuedRepo, times(3)).existsByVerificationCode(anyString());
  }

  @Test
  void runGeneration_completesWithZeroRecipients_andNoAssets() throws Exception {
    CertificateGenerationRun run = run("ALL_PARTICIPANTS", null);
    stubRunLookup(run);
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(templateRepo.findById(templateId)).thenReturn(Optional.of(template(null)));
    when(recipientResolver.resolve(event, "ALL_PARTICIPANTS", null)).thenReturn(List.of());

    service.runGeneration(run.getRunId());

    assertThat(run.getStatus()).isEqualTo("COMPLETED");
    assertThat(run.getTotalCount()).isZero();
    verify(storageService, never()).download(anyString(), anyString());
    verify(issuedRepo, never()).save(any());
  }

  @Test
  void runGeneration_skipsAsset_whenItCannotBeRead() throws Exception {
    CertificateGenerationRun run = run("ALL_PARTICIPANTS", null);
    InputStream broken = mock(InputStream.class);
    when(broken.readAllBytes()).thenThrow(new IOException("read failed"));
    stubRunLookup(run);
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(templateRepo.findById(templateId))
        .thenReturn(Optional.of(template(null, el("IMAGE", "logo.png"))));
    when(storageService.download(CONTAINER, "logo.png")).thenReturn(broken);
    when(recipientResolver.resolve(event, "ALL_PARTICIPANTS", null))
        .thenReturn(List.of(recipient("Charles Leclerc", 1, "WINNER")));
    when(certificateGenerator.generate(any(), any(), any(), any(), anyString(), anyString()))
        .thenReturn(new byte[] {1});

    service.runGeneration(run.getRunId());

    assertThat(run.getStatus()).isEqualTo("COMPLETED");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, byte[]>> assets = ArgumentCaptor.forClass(Map.class);
    verify(certificateGenerator)
        .generate(any(), any(), assets.capture(), any(), anyString(), anyString());
    assertThat(assets.getValue()).isEmpty();
  }

  @Test
  void runGeneration_fails_whenEventMissing() {
    CertificateGenerationRun run = run("ALL_PARTICIPANTS", null);
    stubRunLookup(run);
    when(eventRepo.findById(eventId)).thenReturn(Optional.empty());

    service.runGeneration(run.getRunId());

    assertThat(run.getStatus()).isEqualTo("FAILED");
    assertThat(run.getErrorMessage()).isEqualTo("Event not found");
    assertThat(run.getCompletedAt()).isNotNull();
  }

  @Test
  void runGeneration_fails_whenTemplateMissing() {
    CertificateGenerationRun run = run("ALL_PARTICIPANTS", null);
    stubRunLookup(run);
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(templateRepo.findById(templateId)).thenReturn(Optional.empty());

    service.runGeneration(run.getRunId());

    assertThat(run.getStatus()).isEqualTo("FAILED");
    assertThat(run.getErrorMessage()).isEqualTo("Certificate template not found");
  }

  @Test
  void runGeneration_fails_whenBackgroundDownloadThrows() {
    CertificateGenerationRun run = run("ALL_PARTICIPANTS", null);
    stubRunLookup(run);
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(templateRepo.findById(templateId)).thenReturn(Optional.of(template("bg.png")));
    when(storageService.download(CONTAINER, "bg.png")).thenThrow(new RuntimeException("blob down"));

    service.runGeneration(run.getRunId());

    assertThat(run.getStatus()).isEqualTo("FAILED");
    assertThat(run.getErrorMessage()).isEqualTo("blob down");
  }

  @Test
  void runGeneration_fails_andKeepsProgress_whenGeneratorThrows() {
    CertificateGenerationRun run = run("ALL_PARTICIPANTS", null);
    stubRunLookup(run);
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(templateRepo.findById(templateId)).thenReturn(Optional.of(template(null)));
    when(recipientResolver.resolve(event, "ALL_PARTICIPANTS", null))
        .thenReturn(List.of(recipient("Charles Leclerc", 1, "WINNER")));
    when(certificateGenerator.generate(any(), any(), any(), any(), anyString(), anyString()))
        .thenThrow(new IllegalStateException("render failed"));
    when(issuedRepo.existsByVerificationCode(anyString())).thenReturn(false);

    service.runGeneration(run.getRunId());

    assertThat(run.getStatus()).isEqualTo("FAILED");
    assertThat(run.getErrorMessage()).isEqualTo("render failed");
    assertThat(run.getTotalCount()).isEqualTo(1);
    assertThat(run.getCompletedCount()).isZero();
    verify(issuedRepo, never()).save(any());
  }

  // ---------- queries ----------

  @Test
  void getIssuedForEvent_delegatesToRepo() {
    List<CertificateIssued> list = List.of(new CertificateIssued());
    when(issuedRepo.findByEventIdOrderByIssuedAtDesc(eventId)).thenReturn(list);
    assertThat(service.getIssuedForEvent(eventId)).isSameAs(list);
  }

  @Test
  void getRun_returnsRun_orThrows() {
    CertificateGenerationRun run = run("ALL_PARTICIPANTS", null);
    when(runRepo.findById(run.getRunId())).thenReturn(Optional.of(run));
    assertThat(service.getRun(run.getRunId())).isSameAs(run);

    UUID other = UUID.randomUUID();
    when(runRepo.findById(other)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getRun(other))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Generation run not found");
  }

  @Test
  void getRunsForEvent_delegatesToRepo() {
    List<CertificateGenerationRun> runs = List.of(run("ALL_PARTICIPANTS", null));
    when(runRepo.findByEventIdOrderByRequestedAtDesc(eventId)).thenReturn(runs);
    assertThat(service.getRunsForEvent(eventId)).isSameAs(runs);
  }

  @Test
  void getIssuedForUser_delegatesToRepo() {
    UUID userId = UUID.randomUUID();
    List<UUID> teams = List.of(UUID.randomUUID());
    List<CertificateIssued> list = List.of(new CertificateIssued());
    when(issuedRepo.findByUserIdOrTeamIdInOrderByIssuedAtDesc(userId, teams)).thenReturn(list);
    assertThat(service.getIssuedForUser(userId, teams)).isSameAs(list);
  }

  @Test
  void getIssued_returnsCertificate_orThrows() {
    UUID id = UUID.randomUUID();
    CertificateIssued cert = new CertificateIssued();
    when(issuedRepo.findById(id)).thenReturn(Optional.of(cert));
    assertThat(service.getIssued(id)).isSameAs(cert);

    UUID other = UUID.randomUUID();
    when(issuedRepo.findById(other)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getIssued(other))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Certificate not found");
  }

  @Test
  void resolveDownloadUrl_usesFriendlyFileName() {
    CertificateIssued cert = new CertificateIssued();
    cert.setStorageKey("certificates/x.pdf");
    cert.setRecipientName("Charles Leclerc");
    when(storageService.generatePresignedUrl(
            CONTAINER, "certificates/x.pdf", 60, "certificate-Charles-Leclerc.pdf"))
        .thenReturn("http://dl");

    assertThat(service.resolveDownloadUrl(cert)).isEqualTo("http://dl");
  }

  // ---------- verify ----------

  private CertificateIssued issued() {
    CertificateIssued cert = new CertificateIssued();
    cert.setEventId(eventId);
    cert.setRecipientName("Charles Leclerc");
    cert.setCertificateType("WINNER");
    cert.setRankAtIssue(1);
    return cert;
  }

  @Test
  void verify_valid_returnsDetails() {
    CertificateIssued cert = issued();
    when(issuedRepo.findByVerificationCode("CODE")).thenReturn(Optional.of(cert));
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));

    CertificateVerificationResponse r = service.verify("CODE");

    assertThat(r.isValid()).isTrue();
    assertThat(r.getRecipientName()).isEqualTo("Charles Leclerc");
    assertThat(r.getEventName()).isEqualTo("Spring Hack");
    assertThat(r.getCertificateType()).isEqualTo("WINNER");
    assertThat(r.getRankAtIssue()).isEqualTo(1);
    assertThat(r.getIssuedAt()).isEqualTo(cert.getIssuedAt());
  }

  @Test
  void verify_valid_butEventDeleted_usesPlaceholderName() {
    when(issuedRepo.findByVerificationCode("CODE")).thenReturn(Optional.of(issued()));
    when(eventRepo.findById(eventId)).thenReturn(Optional.empty());

    CertificateVerificationResponse r = service.verify("CODE");

    assertThat(r.isValid()).isTrue();
    assertThat(r.getEventName()).isEqualTo("Unknown event");
  }

  @Test
  void verify_unknownCode_returnsInvalid() {
    when(issuedRepo.findByVerificationCode("NOPE")).thenReturn(Optional.empty());

    CertificateVerificationResponse r = service.verify("NOPE");

    assertThat(r.isValid()).isFalse();
    assertThat(r.getRecipientName()).isNull();
    verify(eventRepo, never()).findById(any());
  }
}
