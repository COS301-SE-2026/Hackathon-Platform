package com.hackathon.platform.controller;

import com.hackathon.platform.dto.CertificateGenerationRunResponse;
import com.hackathon.platform.dto.CertificateIssuedResponse;
import com.hackathon.platform.dto.CertificateTemplateRequest;
import com.hackathon.platform.dto.CertificateTemplateResponse;
import com.hackathon.platform.dto.GenerateCertificatesRequest;
import com.hackathon.platform.model.CertificateGenerationRun;
import com.hackathon.platform.model.CertificateIssued;
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
import com.hackathon.platform.dto.CertificateAssetResponse;

@RestController
@RequestMapping
public class AdminCertificateController {
    private final CertificateService certificateService;
    public AdminCertificateController(CertificateService cert){
        this.certificateService = cert;
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CertificateTemplateResponse> createTemplate(@Valid @RequestBody CertificateTemplateRequest req, @AuthenticationPrincipal User user){
        CertificateTemplate template = certificateService.createTemplate(req, user.getUserId());
        return ResponseEntity.ok(toResponse(template));
    }

    @GetMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CertificateTemplateResponse>> getTemplates(@RequestParam UUID eventId, @RequestParam(required = false) UUID hackathonId) {
        List<CertificateTemplateResponse> resp = certificateService.getTemplatesForEvent(eventId, hackathonId).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/templates/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CertificateTemplateResponse> getTemplate(@PathVariable UUID templateId){
        return ResponseEntity.ok(toResponse(certificateService.getTemplate(templateId)));
    }

    @PutMapping("/templates/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CertificateTemplateResponse> updateTemplate(@PathVariable UUID templateId, @Valid @RequestBody CertificateTemplateRequest req){
        CertificateTemplate template = certificateService.updateTemplate(templateId, req);
        return ResponseEntity.ok(toResponse(template));
    }

    @DeleteMapping("/templates{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID templateId){
        certificateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("templates/{templateId}/background")
    @PreAuthorize("hasRole('ADMON')")
    public ResponseEntity<CertificateTemplateResponse> uploadBackground(@PathVariable UUID templateId, @RequestParam("file") MultipartFile file){
        certificateService.uploadBackground(templateId, file);
        return ResponseEntity.ok(toResponse(certificateService.getTemplate(templateId)));
    }


}