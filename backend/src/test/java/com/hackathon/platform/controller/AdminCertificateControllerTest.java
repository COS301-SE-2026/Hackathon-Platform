package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.CertificateAssetResponse;
import com.hackathon.platform.dto.CertificateGenerationRunResponse;
import com.hackathon.platform.dto.CertificateIssuedResponse;
import com.hackathon.platform.dto.CertificateTemplateRequest;
import com.hackathon.platform.dto.CertificateTemplateResponse;
import com.hackathon.platform.dto.GenerateCertificatesRequest;
import com.hackathon.platform.model.CertificateGenerationRun;
import com.hackathon.platform.model.CertificateIssued;
import com.hackathon.platform.model.CertificateLayout;
import com.hackathon.platform.model.CertificateTemplate;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CertificateService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AdminCertificateControllerTest {
  @Mock private CertificateService certificateService;
  private AdminCertificateController controller;
  private User admin;
  private UUID templateId;
  private UUID eventId;
  private CertificateTemplate template;

  @BeforeEach
  void setUp() {
    controller = new AdminCertificateController(certificateService);
    admin = User.builder().userId(UUID.randomUUID()).build();
    templateId = UUID.randomUUID();
    eventId = UUID.randomUUID();
    template = new CertificateTemplate();
    template.setTemplateId(templateId);
    template.setName("Tpl");
    template.setLayout(new CertificateLayout());
  }

  private void stubResponseParts() {
    when(certificateService.resolveBackgroundUrl(template)).thenReturn("http://bg");
    when(certificateService.resolveTemplateAssetUrls(template)).thenReturn(Map.of("k", "http://k"));
  }

  private void assertTemplateResponse(CertificateTemplateResponse r) {
    assertThat(r.getTemplateId()).isEqualTo(templateId);
    assertThat(r.getName()).isEqualTo("Tpl");
    assertThat(r.getBackgroundUrl()).isEqualTo("http://bg");
    assertThat(r.getAssetUrls()).containsEntry("k", "http://k");
  }

  private CertificateGenerationRun run() {
    CertificateGenerationRun run = new CertificateGenerationRun();
    run.setRunId(UUID.randomUUID());
    run.setEventId(eventId);
    run.setTemplateId(templateId);
    run.setScope("ALL_PARTICIPANTS");
    return run;
  }

  @Test
  void createTemplate_returnsMappedTemplate() {
    CertificateTemplateRequest req = new CertificateTemplateRequest();
    when(certificateService.createTemplate(req, admin.getUserId())).thenReturn(template);
    stubResponseParts();

    ResponseEntity<CertificateTemplateResponse> resp = controller.createTemplate(req, admin);

    assertThat(resp.getStatusCode().value()).isEqualTo(200);
    assertTemplateResponse(resp.getBody());
  }

  @Test
  void getTemplates_mapsEveryTemplate() {
    UUID hackId = UUID.randomUUID();
    when(certificateService.getTemplatesForEvent(eventId, hackId)).thenReturn(List.of(template));
    stubResponseParts();

    ResponseEntity<List<CertificateTemplateResponse>> resp = controller.getTemplates(eventId, hackId);

    assertThat(resp.getBody()).hasSize(1);
    assertTemplateResponse(resp.getBody().get(0));
  }

  @Test
  void getTemplates_worksWithoutHackathonId() {
    when(certificateService.getTemplatesForEvent(eventId, null)).thenReturn(List.of());

    assertThat(controller.getTemplates(eventId, null).getBody()).isEmpty();
  }

  @Test
  void getTemplate_returnsMappedTemplate() {
    when(certificateService.getTemplate(templateId)).thenReturn(template);
    stubResponseParts();

    assertTemplateResponse(controller.getTemplate(templateId).getBody());
  }

  @Test
  void updateTemplate_returnsMappedTemplate() {
    CertificateTemplateRequest req = new CertificateTemplateRequest();
    when(certificateService.updateTemplate(templateId, req)).thenReturn(template);
    stubResponseParts();

    assertTemplateResponse(controller.updateTemplate(templateId, req).getBody());
  }

  @Test
  void deleteTemplate_returns204() {
    ResponseEntity<Void> resp = controller.deleteTemplate(templateId);

    assertThat(resp.getStatusCode().value()).isEqualTo(204);
    verify(certificateService).deleteTemplate(templateId);
  }

  @Test
  void uploadBackground_uploadsThenReturnsRefreshedTemplate() {
    MockMultipartFile file = new MockMultipartFile("file", "bg.png", "image/png", new byte[] {1});
    when(certificateService.getTemplate(templateId)).thenReturn(template);
    stubResponseParts();

    ResponseEntity<CertificateTemplateResponse> resp = controller.uploadBackground(templateId, file);

    verify(certificateService).uploadBackground(templateId, file);
    assertTemplateResponse(resp.getBody());
  }

  @Test
  void uploadAsset_returnsKeyAndPresignedUrl() {
    MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1});
    when(certificateService.uploadTemplateAsset(templateId, file)).thenReturn("assets/logo.png");
    when(certificateService.resolveAssetUrl("assets/logo.png")).thenReturn("http://logo");

    CertificateAssetResponse body = controller.uploadAsset(templateId, file).getBody();

    assertThat(body.getStorageKey()).isEqualTo("assets/logo.png");
    assertThat(body.getUrl()).isEqualTo("http://logo");
  }

  @Test
  void generate_startsRunForAuthenticatedAdmin() {
    GenerateCertificatesRequest req = new GenerateCertificatesRequest();
    req.setTemplateId(templateId);
    req.setScope("TOP_N");
    req.setTopN(3);
    CertificateGenerationRun run = run();
    when(certificateService.startGeneration(eventId, templateId, "TOP_N", 3, admin.getUserId()))
        .thenReturn(run);

    ResponseEntity<CertificateGenerationRunResponse> resp = controller.generate(eventId, req, admin);

    assertThat(resp.getStatusCode().value()).isEqualTo(200);
    assertThat(resp.getBody().getRunId()).isEqualTo(run.getRunId());
  }

  @Test
  void getRuns_mapsRuns() {
    CertificateGenerationRun run = run();
    when(certificateService.getRunsForEvent(eventId)).thenReturn(List.of(run));

    List<CertificateGenerationRunResponse> body = controller.getRuns(eventId).getBody();

    assertThat(body).hasSize(1);
    assertThat(body.get(0).getRunId()).isEqualTo(run.getRunId());
  }

  @Test
  void getRun_mapsRun() {
    CertificateGenerationRun run = run();
    when(certificateService.getRun(run.getRunId())).thenReturn(run);

    assertThat(controller.getRun(run.getRunId()).getBody().getRunId()).isEqualTo(run.getRunId());
  }

  @Test
  void getIssued_includesDownloadUrlForEachCertificate() {
    CertificateIssued cert = new CertificateIssued();
    cert.setCertificateId(UUID.randomUUID());
    cert.setRecipientName("Charles Leclerc");
    when(certificateService.getIssuedForEvent(eventId)).thenReturn(List.of(cert));
    when(certificateService.resolveDownloadUrl(cert)).thenReturn("http://dl");

    List<CertificateIssuedResponse> body = controller.getIssued(eventId).getBody();

    assertThat(body).hasSize(1);
    assertThat(body.get(0).getDownloadUrl()).isEqualTo("http://dl");
    assertThat(body.get(0).getRecipientName()).isEqualTo("Charles Leclerc");
  }
}
