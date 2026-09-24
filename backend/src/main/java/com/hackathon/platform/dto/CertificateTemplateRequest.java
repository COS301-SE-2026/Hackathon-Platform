package com.hackathon.platform.dto;

import com.hackathon.platform.model.CertificateLayout;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificateTemplateRequest {
  @NotBlank private String name;
  private UUID eventId;
  private UUID hackathonId;
  @NotNull private CertificateLayout layout;
}
