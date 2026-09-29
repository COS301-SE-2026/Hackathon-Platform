package com.hackathon.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.CertificateVerificationResponse;
import com.hackathon.platform.model.CertificateIssued;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.service.CertificateService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CertificateControllerTest {
  @Mock private CertificateService certificateService;
  @Mock private TeamMemberRepository teamMemberRepo;
  private CertificateController controller;

  @BeforeEach
  void setUp() {
    controller = new CertificateController(certificateService, teamMemberRepo);
  }

  @Test
  void downloadCertificate_redirectsToPresignedUrl() {
    UUID id = UUID.randomUUID();
    CertificateIssued cert = new CertificateIssued();
    when(certificateService.getIssued(id)).thenReturn(cert);
    when(certificateService.resolveDownloadUrl(cert)).thenReturn("http://dl");

    ResponseEntity<Void> resp = controller.downloadCertificate(id);

    assertThat(resp.getStatusCode().value()).isEqualTo(302);
    assertThat(resp.getHeaders().getFirst("Location")).isEqualTo("http://dl");
  }

  @Test
  void downloadCertificate_propagatesNotFound() {
    UUID id = UUID.randomUUID();
    when(certificateService.getIssued(id)).thenThrow(new IllegalArgumentException("Certificate not found"));
    assertThatThrownBy(() -> controller.downloadCertificate(id))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void verify_returns200WithServiceResult() {
    CertificateVerificationResponse body = CertificateVerificationResponse.invalid();
    when(certificateService.verify("CODE")).thenReturn(body);

    ResponseEntity<CertificateVerificationResponse> resp = controller.verify("CODE");

    assertThat(resp.getStatusCode().value()).isEqualTo(200);
    assertThat(resp.getBody()).isSameAs(body);
  }
}
