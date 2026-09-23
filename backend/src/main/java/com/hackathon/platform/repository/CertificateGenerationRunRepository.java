package com.hackathon.platform.repository;

import com.hackathon.platform.model.CertificateGenerationRun;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateGenerationRunRepository extends JpaRepository<CertificateGenerationRun, UUID>{
    List<CertificateGenerationRun> findByEventIdOrderByRequestedAtDesc(UUID eventId);
}