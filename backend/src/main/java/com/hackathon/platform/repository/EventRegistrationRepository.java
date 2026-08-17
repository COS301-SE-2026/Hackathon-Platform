package com.hackathon.platform.repository;

import com.hackathon.platform.model.EventRegistration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {
  boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

  Optional<EventRegistration> findEventIdAndUserId(UUID eventId, UUID userId);

  List<EventRegistration> findByUserId(UUID userId);
}
