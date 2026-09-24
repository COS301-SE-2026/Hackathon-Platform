package com.hackathon.platform.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateCertificatesRequest {
    @NotNull private UUID templateId;
    private String scope = "ALL_PARTICIPANTS";
    private Integer topN;
}