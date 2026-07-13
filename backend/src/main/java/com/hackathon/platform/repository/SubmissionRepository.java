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

  @Query(
      "SELECT s FROM Submission s WHERE s.teamId = :teamId AND s.levelId = :levelId ORDER BY s.submittedAt DESC")
  List<Submission> findLatestByTeamAndLevel(
      @Param("teamId") UUID teamId, @Param("levelId") Long levelId);

  @Query("SELECT s FROM Submission s WHERE s.status = :status ORDER BY s.submittedAt ASC")
  List<Submission> findByStatusOrderBySubmittedAtAsc(@Param("status") String status);

  @Query(value =
      """
        WITH BestSubmissions AS (
            SELECT DISTINCT ON (team_id) team_id, score, submitted_at 
            FROM submissions s 
            WHERE level_id = :levelId AND status = 'SCORED' 
            ORDER BY team_id, score DESC, submitted_at ASC
        )
        SELECT 
            team.team_id AS "teamId", 
            team.team_name AS "teamName", 
            COALESCE(best.score, CAST(0 AS NUMERIC)) AS "bestScore", 
            best.submitted_at AS "lastScoredAt"
        FROM teams team 
            LEFT JOIN BestSubmissions best ON team.team_id = best.team_id WHERE team.event_id = :eventId AND team.status = 'ACTIVE' ORDER BY "bestScore" DESC
        """,
      nativeQuery = true)
  List<LeaderboardEntry> findLeaderboardByEventIdAndLevelId(
      @Param("eventId") UUID eventId, @Param("levelId") Long levelId);

  boolean existsByOutputStorageKey(String storageKey);

  boolean existsBySourceCodeStorageKey(String storageKey);
}
