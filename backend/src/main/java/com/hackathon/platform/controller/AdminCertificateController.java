package com.hackathon.platform.controller;

import com.hackathon.platform.dto.CertificateAssetResponse;
import com.hackathon.platform.dto.CertificateGenerationRunResponse;
import com.hackathon.platform.dto.CertificateIssuedResponse;
import com.hackathon.platform.dto.CertificateTemplateRequest;
import com.hackathon.platform.dto.CertificateTemplateResponse;
import com.hackathon.platform.dto.GenerateCertificatesRequest;
import com.hackathon.platform.model.CertificateGenerationRun;
import com.hackathon.platform.model.CertificateTemplate;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CertificateService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/certificates")
public class AdminCertificateController {
  private final CertificateService certificateService;

  public AdminCertificateController(CertificateService cert) {
    this.certificateService = cert;
  }

  @PostMapping("/templates")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CertificateTemplateResponse> createTemplate(
      @Valid @RequestBody CertificateTemplateRequest req, @AuthenticationPrincipal User user) {
    CertificateTemplate template = certificateService.createTemplate(req, user.getUserId());
    return ResponseEntity.ok(toResponse(template));
  }

  @GetMapping("/templates")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<CertificateTemplateResponse>> getTemplates(
      @RequestParam UUID eventId, @RequestParam(required = false) UUID hackathonId) {
    List<CertificateTemplateResponse> resp =
        certificateService.getTemplatesForEvent(eventId, hackathonId).stream()
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(resp);
  }

  @GetMapping("/templates/{templateId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CertificateTemplateResponse> getTemplate(@PathVariable UUID templateId) {
    return ResponseEntity.ok(toResponse(certificateService.getTemplate(templateId)));
  }

  @PutMapping("/templates/{templateId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CertificateTemplateResponse> updateTemplate(
      @PathVariable UUID templateId, @Valid @RequestBody CertificateTemplateRequest req) {
    CertificateTemplate template = certificateService.updateTemplate(templateId, req);
    return ResponseEntity.ok(toResponse(template));
  }

  @DeleteMapping("/templates/{templateId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteTemplate(@PathVariable UUID templateId) {
    certificateService.deleteTemplate(templateId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/templates/{templateId}/background")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CertificateTemplateResponse> uploadBackground(
      @PathVariable UUID templateId, @RequestParam("file") MultipartFile file) {
    certificateService.uploadBackground(templateId, file);
    return ResponseEntity.ok(toResponse(certificateService.getTemplate(templateId)));
  }

  @PostMapping("/templates/{templateId}/assets")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CertificateAssetResponse> uploadAsset(
      @PathVariable UUID templateId, @RequestParam("file") MultipartFile file) {
    String storageKey = certificateService.uploadTemplateAsset(templateId, file);
    String url = certificateService.resolveAssetUrl(storageKey);
    return ResponseEntity.ok(new CertificateAssetResponse(storageKey, url));
  }

  @PostMapping("/events/{eventId}/generate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CertificateGenerationRunResponse> generate(
      @PathVariable UUID eventId,
      @Valid @RequestBody GenerateCertificatesRequest req,
      @AuthenticationPrincipal User user) {
    CertificateGenerationRun run =
        certificateService.startGeneration(
            eventId, req.getTemplateId(), req.getScope(), req.getTopN(), user.getUserId());
    return ResponseEntity.ok(new CertificateGenerationRunResponse(run));
  }

  @GetMapping("/events/{eventId}/runs")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<CertificateGenerationRunResponse>> getRuns(
      @PathVariable UUID eventId) {
    List<CertificateGenerationRunResponse> resp =
        certificateService.getRunsForEvent(eventId).stream()
            .map(CertificateGenerationRunResponse::new)
            .toList();
    return ResponseEntity.ok(resp);
  }

  @GetMapping("/runs/{runId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CertificateGenerationRunResponse> getRun(@PathVariable UUID runId) {
    return ResponseEntity.ok(
        new CertificateGenerationRunResponse(certificateService.getRun(runId)));
  }

  @GetMapping("/events/{eventId}/issued")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<CertificateIssuedResponse>> getIssued(@PathVariable UUID eventId) {
    List<CertificateIssuedResponse> resp =
        certificateService.getIssuedForEvent(eventId).stream()
            .map(
                cert ->
                    new CertificateIssuedResponse(cert, certificateService.resolveDownloadUrl(cert)))
            .toList();
    return ResponseEntity.ok(resp);
  }

  private CertificateTemplateResponse toResponse(CertificateTemplate template) {
    return new CertificateTemplateResponse(
        template,
        certificateService.resolveBackgroundUrl(template),
        certificateService.resolveTemplateAssetUrls(template));
  }
}
