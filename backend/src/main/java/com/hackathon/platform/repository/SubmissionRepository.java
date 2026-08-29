package com.hackathon.platform.repository;

import com.hackathon.platform.model.Submission;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

  List<Submission> findByTeamId(UUID teamId);

  List<Submission> findByLevelId(short levelId);

  Optional<Submission> findByIdAndTeamId(Long id, UUID teamId);

  @Query(
      "SELECT s FROM Submission s WHERE s.teamId = :teamId AND s.levelId = :levelId ORDER BY s.submittedAt DESC")
  List<Submission> findLatestByTeamAndLevel(
      @Param("teamId") UUID teamId, @Param("levelId") short levelId);

  @Query("SELECT s FROM Submission s WHERE s.status = :status ORDER BY s.submittedAt ASC")
  List<Submission> findByStatusOrderBySubmittedAtAsc(@Param("status") String status);

  @Query(
      "SELECT s FROM Submission s, Event e WHERE s.eventId = e.eventId AND e.createdByUserId = :userId ORDER BY s.submittedAt DESC")
  List<Submission> getRecentSubmissions(@Param("userId") UUID userId, Pageable pageeable);

  @Query(
      "SELECT s.id FROM Submission s, Event e WHERE s.eventId = e.eventId AND e.hackathon = :hackathonId")
  List<Long> findIdsByHackathonId(@Param("hackathonId") UUID hackathonId);

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
      "UPDATE Submission s SET s.solverVersionId = :solverVersionId WHERE s.id IN "
          + "(SELECT s2.id FROM Submission s2, Event e WHERE s2.eventId = e.eventId AND e.hackathon = :hackathonId)")
  int updateSolverVersionForHackathon(
      @Param("hackathonId") UUID hackathonId, @Param("solverVersionId") Long solverVersionId);

  @Query(
      value =
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
            LEFT JOIN BestSubmissions best ON team.team_id = best.team_id WHERE team.event_id = :eventId AND team.status = 'ACTIVE' ORDER BY "bestScore" DESC, "lastScoredAt" ASC NULLS LAST
        """,
      nativeQuery = true)
  List<LeaderboardEntry> findLeaderboardByEventIdAndLevelId(
      @Param("eventId") UUID eventId, @Param("levelId") short levelId);

  @Query(
      value =
          """
        WITH BestSubmissions AS (
            SELECT DISTINCT ON (s.team_id, s.level_id) s.team_id, s.level_id, s.score, s.submitted_at
            FROM submissions s
            INNER JOIN teams t ON t.team_id = s.team_id
            WHERE t.event_id = :eventId AND s.status = 'SCORED'
            ORDER BY s.team_id, s.level_id, s.score DESC, s.submitted_at ASC, s.id ASC
        ),
        TeamTotals AS (
            SELECT
                team_id,
                sum(score) AS total_score,
                MAX(submitted_at) AS last_scored_at
            FROM BestSubmissions
            GROUP BY team_id
        )
        SELECT
        t.team_id AS "teamId",
        t.team_name AS "teamName",
        COALESCE(totals.total_score, 0) AS "bestScore", totals.last_scored_at AS "lastScoredAt"
        FROM teams t
        LEFT JOIN TeamTotals totals ON t.team_id = totals.team_id
        WHERE t.event_id = :eventId AND t.status = 'ACTIVE'
        ORDER BY
            "bestScore" DESC,
            totals.last_scored_at ASC NULLS LAST,
            t.team_name ASC,
            t.team_id ASC

        """,
      nativeQuery = true)
  List<LeaderboardEntry> findLeaderboardByEventId(@Param("eventId") UUID eventId);

  boolean existsByOutputStorageKey(String storageKey);

  boolean existsBySourceCodeStorageKey(String storageKey);

  long countByEventId(UUID eventId);

  @Query(
      "SELECT COUNT(s) FROM Submission s, Event e WHERE s.eventId = e.eventId AND e.createdByUserId = :userId AND s.submittedAt >= :since")
  long countByAdminSince(@Param("userId") UUID userId, @Param("since") Instant since);

  long countByEventIdAndSubmittedAtAfter(UUID eventId, Instant since);

  @Query(
      "SELECT s.status AS status, COUNT(s) AS total FROM Submission s WHERE s.eventId = :eventId GROUP BY s.status")
  List<StatusCount> countByEventIdGroupByStatus(@Param("eventId") UUID eventId);

  @Query(
      value =
          """
       SELECT date_trunc('minute', submitted_at) AS "bucketStart", COUNT(*) AS "count"
       FROM submissions
       WHERE event_id = :eventId AND submitted_at >= :since
       GROUP BY date_trunc('minute', submitted_at)
       ORDER BY "bucketStart" ASC
        """,
      nativeQuery = true)
  List<SubmissionRateRow> findSubmissionRateSince(
      @Param("eventId") UUID eventId, @Param("since") Instant since);

  @Query(
      value =
          """
       SELECT
            s.level_id AS "levelId",
            l.name AS "levelName",
            COUNT(*) AS "scoredSubmissions",
            MIN(s.score) AS "minScore",
            MAX(s.score) AS "maxScore",
            AVG(s.score) AS "avgScore"
       FROM submissions s
       JOIN levels l ON l.id = s.level_id
       WHERE s.event_id = :eventId AND s.status = 'SCORED'
       GROUP BY s.level_id, l.name
       ORDER BY s.level_id ASC
        """,
      nativeQuery = true)
  List<LevelScoreRow> findScoreDistributionByEventId(@Param("eventId") UUID eventId);

  long countByEventIdAndStatus(UUID eventId, String status);

  /** Projection for gtoup by status counts */
  interface StatusCount {
    String getStatus();

    long getTotal();
  }

  /** Projection for the submission rate time series */
  interface SubmissionRateRow {

    Instant getBucketStart();

    long getCount();
  }

  /** Projection for the per level score distribution */
  interface LevelScoreRow {
    short getLevelId();

    String getLevelName();

    long getScoredSubmissions();

    java.math.BigDecimal getMinScore();

    java.math.BigDecimal getMaxScore();

    java.math.BigDecimal getAvgScore();
  }
}
