package com.hackathon.platform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificateAssetResponse {
  private final String storageKey;
  private final String url;

  public CertificateAssetResponse(String storageKey, String url) {
    this.storageKey = storageKey;
    this.url = url;
  }
}
