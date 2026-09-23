package com.hackathon.platform.repository;

import com.hackathon.platform.model.CertificateTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, UUID>{
    List<CertificateTemplate> findByEventId(UUID eventId);
    List<CertificateTemplate> findByHackathonIdAndEventIdIsNull(UUID hackathonId);
}