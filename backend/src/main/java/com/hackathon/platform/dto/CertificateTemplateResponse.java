package com.hackathon.platform.dto;

import com.hackathon.platform.model.CertificateLayout;
import com.hackathon.platform.model.CertificateTemplate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificateTemplateResponse{
    private final UUID templateId;
    private final UUID eventId;
    private final UUID hackathonId;
    private final String name;
    private final String backgroundUrl;
    private final CertificateLayout layout;
    private final Map<String, String> assetUrls;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public CertificateTemplateResponse(CertificateTemplate template, String backgroundUrl, Map<String, String> assetUrls){
        this.templateId = template.getTemplateId();
        this.eventId = template.getEventId();
        this.hackathonId = template.getHackathonId();
        this.name = template.getName();
        this.backgroundUrl = backgroundUrl;
        this.layout = template.getLayout();
        this.assetUrls = assetUrls;
        this.createdAt = template.getCreatedAt();
        this.updatedAt = template.getUpdatedAt();
    }
}