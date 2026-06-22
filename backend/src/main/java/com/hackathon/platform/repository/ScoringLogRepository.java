package com.hackathon.platform.repository;

import com.hackathon.platform.model.ScoringLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScoringLogRepository extends JpaRepository<ScoringLog, Long> {

  @Query(
      "SELECT sl FROM ScoringLog sl WHERE sl.submissionId = :submissionId"
          + " ORDER BY sl.createdAt ASC")
  List<ScoringLog> findBySubmissionIdOrderByCreatedAtAsc(
      @Param("submissionId") Long submissionId);

  void deleteBySubmissionId(Long submissionId);
}
