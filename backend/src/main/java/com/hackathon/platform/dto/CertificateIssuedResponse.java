package com.hackathon.platform.dto;

import com.hackathon.platform.model.CertificateIssued;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificateIssuedResponse {
  private final UUID certificateId;
  private final UUID eventId;
  private final UUID teamId;
  private final UUID userId;
  private final String CertificateType;
  private final String recipientName;
  private final Integer rankAtIssue;
  private final String verificationCode;
  private final String downloadUrl;
  private final OffsetDateTime issuedAt;

  public CertificateIssuedResponse(CertificateIssued cert, String downloadUrl) {
    this.certificateId = cert.getCertificateId();
    this.eventId = cert.getEventId();
    this.teamId = cert.getTeamId();
    this.userId = cert.getUserId();
    this.CertificateType = cert.getCertificateType();
    this.recipientName = cert.getRecipientName();
    this.rankAtIssue = cert.getRankAtIssue();
    this.verificationCode = cert.getVerificationCode();
    this.downloadUrl = downloadUrl;
    this.issuedAt = cert.getIssuedAt();
  }
}
