package com.hackathon.platform.dto;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificateVerificationResponse {
  private final boolean valid;
  private final String recipientName;
  private final String eventName;
  private final String certificateType;
  private final Integer rankAtIssue;
  private final OffsetDateTime issuedAt;

  public CertificateVerificationResponse(
      boolean valid,
      String recipientName,
      String eventName,
      String certificateType,
      Integer rankAtIssue,
      OffsetDateTime issuedAt) {
    this.valid = valid;
    this.recipientName = recipientName;
    this.eventName = eventName;
    this.certificateType = certificateType;
    this.rankAtIssue = rankAtIssue;
    this.issuedAt = issuedAt;
  }

  public static CertificateVerificationResponse invalid(){
    return new CertificateVerificationResponse(false, null, null, null, null, null);
  }
}
