package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "certificate_template")
@Getter
@Setter
public class CertificateTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "template_id", updatable = false, nullable = false)
    private UUID templateId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "hackathon_id")
    private UUID hackathonId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "background_storage_key", columnDefinition = "TEXT")
    private String backgroundStorageKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable  = false, columnDefinition = "jsonb")
    private CertificateLayout layout;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}