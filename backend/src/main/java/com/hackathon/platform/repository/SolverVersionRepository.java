package com.hackathon.platform.repository;

import com.hackathon.platform.model.SolverVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolverVersionRepository extends JpaRepository<SolverVersion, Long> {

  List<SolverVersion> findByEventId(UUID eventId);

  Optional<SolverVersion> findByEventIdAndIsActiveTrue(UUID eventId);

  @Query("SELECT sv FROM SolverVersion sv WHERE sv.eventId = :eventId ORDER BY sv.versionNumber DESC")
  List<SolverVersion> findAllByEventIdOrderByVersionDesc(@Param("eventId") UUID eventId);

  Optional<SolverVersion> findByEventIdAndVersionNumber(UUID eventId, Integer versionNumber);

  boolean existsByStorageKey(String storageKey);
}