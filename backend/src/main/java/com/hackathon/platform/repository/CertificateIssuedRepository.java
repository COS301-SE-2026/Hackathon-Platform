package com.hackathon.platform.repository;

import com.hackathon.platform.model.CertificateIssued;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateIssuedRepository extends JpaRepository<CertificateIssued, UUID>{
    List<CertificateIssued> findByRunId(UUID runId);
    List<CertificateIssued> findByEventIdOrderByIssuedAtDesc(UUID eventId);
    List<CertificateIssued> findByUserIdOrTeamIdInOrderByIssuedAtDesc(UUID userId, List<UUID> teamIds);
    Optional<CertificateIssued> findByVerificationCode(String verificationCode);
    boolean existsByVerificationCode(String verificationCode);
}