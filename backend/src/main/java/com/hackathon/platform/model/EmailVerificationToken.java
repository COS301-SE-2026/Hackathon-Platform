package com.hackathon.platform.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_tokens")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken{
    @Getter
    @Id
    private UUID id;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Getter
    @Setter
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Getter
    @Setter
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Getter
    @Setter
    @Column(nullable = false)
    private boolean used = false;
}