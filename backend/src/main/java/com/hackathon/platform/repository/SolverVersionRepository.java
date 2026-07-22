package com.hackathon.platform.repository;

import com.hackathon.platform.model.SolverVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolverVersionRepository extends JpaRepository<SolverVersion, Long> {

  List<SolverVersion> findByHackathonId(UUID hackathonId);

  Optional<SolverVersion> findByHackathonIdAndIsActiveTrue(UUID hackathonId);

  /** The most recently uploaded solver version for a hackathon (highest version number). */
  Optional<SolverVersion> findFirstByHackathonIdOrderByVersionNumberDesc(UUID hackathonId);

  @Query(
      "SELECT sv FROM SolverVersion sv WHERE sv.hackathonId = :hackathonId ORDER BY sv.versionNumber DESC")
  List<SolverVersion> findAllByHackathonIdOrderByVersionDesc(
      @Param("hackathonId") UUID hackathonId);

  Optional<SolverVersion> findByHackathonIdAndVersionNumber(
      UUID hackathonId, Integer versionNumber);

  boolean existsByStorageKey(String storageKey);
}