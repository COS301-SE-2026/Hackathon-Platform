package com.hackathon.platform.repository;

import com.hackathon.platform.model.Submission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

  List<Submission> findByTeamId(UUID teamId);

  List<Submission> findByLevelId(Long levelId);

  Optional<Submission> findByIdAndTeamId(Long id, UUID teamId);

  @Query("SELECT s FROM Submission s WHERE s.teamId = :teamId AND s.levelId = :levelId ORDER BY s.submittedAt DESC")
  List<Submission> findLatestByTeamAndLevel(@Param("teamId") UUID teamId, @Param("levelId") Long levelId);

  @Query("SELECT s FROM Submission s WHERE s.status = :status ORDER BY s.submittedAt ASC")
  List<Submission> findByStatusOrderBySubmittedAtAsc(@Param("status") String status);

  boolean existsByOutputStorageKey(String storageKey);

  boolean existsBySourceCodeStorageKey(String storageKey);
}