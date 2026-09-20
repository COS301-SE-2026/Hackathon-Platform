package com.hackathon.platform.repository;

import com.hackathon.platform.model.EmailVerificationToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, UUID> {
  Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

  void deleteByUserUserId(UUID userId);
}
