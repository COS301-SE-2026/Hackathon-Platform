package com.hackathon.platform.certificate;

import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificateRecipient {
  private final UUID userId;
  private final UUID teamId;
  private final String recipientName;
  private final Integer rank;
  private final String certificateType;
  private final Map<String, String> fieldValues;

  public CertificateRecipient(
      UUID userId,
      UUID teamId,
      String recipientName,
      Integer rank,
      String certificateType,
      Map<String, String> fieldValues) {
    this.userId = userId;
    this.teamId = teamId;
    this.recipientName = recipientName;
    this.rank = rank;
    this.certificateType = certificateType;
    this.fieldValues = fieldValues;
  }
}
