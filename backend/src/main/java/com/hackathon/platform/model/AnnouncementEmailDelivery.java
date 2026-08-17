package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@IdClass(AnnouncementEmailDeliveryId.class)
@Table(name = "announcement_email_deliveries")
@Getter
@Setter
@NoArgsConstructor
public class AnnouncementEmailDelivery {
    @Id
    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "delivery_status", nullable = false, length = 20)
    private String deliveryStatus = "PENDING";

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}