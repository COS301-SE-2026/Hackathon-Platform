package com.hackathon.platform.controller;

import com.hackathon.platform.dto.*;
import com.hackathon.platform.model.CertificateIssued;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.service.CertificateService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CertificateController {
  private final CertificateService certificateService;
  private final TeamMemberRepository teamMemberRepo;

  public CertificateController(
      CertificateService certificateService, TeamMemberRepository teamMemRepo) {
    this.certificateService = certificateService;
    this.teamMemberRepo = teamMemRepo;
  }

  @GetMapping("/api/certificates/{certificateId}/download")
  public ResponseEntity<Void> downloadCertificate(@PathVariable UUID certificateId) {
    CertificateIssued cert = certificateService.getIssued(certificateId);
    String url = certificateService.resolveDownloadUrl(cert);
    return ResponseEntity.status(302).header("Location", url).build();
  }

  @GetMapping("/api/certificates/verify/{verificationCode}")
  public ResponseEntity<CertificateVerificationResponse> verify(
      @PathVariable String verificationCode) {
    return ResponseEntity.ok(certificateService.verify(verificationCode));
  }
}
