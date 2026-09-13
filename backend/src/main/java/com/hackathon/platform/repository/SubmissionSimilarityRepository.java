package com.hackathon.platform.repository;

import com.hackathon.platform.model.SubmissionSimilarity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionSimilarityRepository extends JpaRepository<SubmissionSimilarity, Long> {

  Optional<SubmissionSimilarity> findBySubmissionIdAAndSubmissionIdB(Long a, Long b);


  List<SubmissionSimilarity> findByEventIdAndLevelIdOrderByCombinedScoreDesc(
      UUID eventId, short levelId);

  List<SubmissionSimilarity> findByEventIdOrderByCombinedScoreDesc(UUID eventId);

  List<SubmissionSimilarity> findByEventIdAndLevelIdAndFlaggedTrueOrderByCombinedScoreDesc(
      UUID eventId, short levelId);

  @Modifying
  @Query(
      "DELETE FROM SubmissionSimilarity s WHERE s.eventId = :eventId AND s.levelId = :levelId")
  void deleteByEventIdAndLevelId(@Param("eventId") UUID eventId, @Param("levelId") short levelId);


}
