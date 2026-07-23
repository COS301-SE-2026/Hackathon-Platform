package com.hackathon.platform.controller;

import com.hackathon.platform.dto.SubmissionResponse;
import com.hackathon.platform.dto.LeaderboardEntryResponse;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.scoring.ScoringService;
import com.hackathon.platform.scoring.SubmissionQueryService;
import com.hackathon.platform.scoring.LeaderboardService;
import com.hackathon.platform.scoring.LeaderboardUpdateService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.hackathon.platform.scoring.queue.ScoringJobProducer;
import java.util.Map;

/**
 * REST endpoints for scoring submissions and retrieving submission history/feedback.
 *
 * <p>Scoring is currently triggered synchronously over HTTP (no queue yet) - call {@code POST
 * /api/scoring/submissions/{id}/score} right after a submission is uploaded via {@code
 * StorageController}, or have an admin call it again to re-score after a solver hotfix.
 */
@RestController
@RequestMapping("/api/scoring")
@RequiredArgsConstructor
public class ScoringController {

  private final ScoringService scoringService;
  private final SubmissionQueryService submissionQueryService;
  private final LeaderboardService leaderboardService;
  private final ScoringJobProducer scoringJobProducer;
  private final LeaderboardUpdateService leaderboardUpdateService;

  /**
   * Triggers scoring for a submission: runs the active solver against the submission's output file
   * and persists score, status and logs. Safe to call again later (e.g. admin-triggered re-scoring
   * after a solver hotfix) - old log entries are preserved, not overwritten.
   *
   * @param submissionId the submission to score
   * @return the updated submission with score/status set
   */
  @PostMapping("/submissions/{submissionId}/score")
  public ResponseEntity<Map<String,String>> scoreSubmission(@PathVariable Long submissionId) {
    String record = scoringJobProducer.enqueue(submissionId);
    return ResponseEntity.accepted().body(Map.of("submissionId", String.valueOf(submissionId), "status", "QUEUED", "recordId", record!=null?record : ""));
  }

  /**
   * Full submission history for a team across all levels, most recent first. Used by the
   * participant portal's "submission history" view.
   *
   * @param teamId the team UUID
   */
  @GetMapping("/teams/{teamId}/submissions")
  public ResponseEntity<List<SubmissionResponse>> getTeamHistory(@PathVariable UUID teamId) {
    return ResponseEntity.ok(submissionQueryService.getHistoryForTeam(teamId));
  }

  /**
   * Submission history for a team, scoped to a single level, most recent first.
   *
   * @param teamId the team UUID
   * @param levelId the level ID
   */
  @GetMapping("/teams/{teamId}/levels/{levelId}/submissions")
  public ResponseEntity<List<SubmissionResponse>> getTeamLevelHistory(
      @PathVariable UUID teamId, @PathVariable Long levelId) {
    return ResponseEntity.ok(submissionQueryService.getHistoryForTeamAndLevel(teamId, levelId));
  }

  /**
   * Full feedback for a single submission, including score, status and every scoring log entry
   * (e.g. malformed output, rule violations, validation failures). Scoped to the owning team so a
   * participant can't view another team's feedback by guessing IDs.
   *
   * @param teamId the team UUID
   * @param submissionId the submission ID
   */
  @GetMapping("/teams/{teamId}/submissions/{submissionId}")
  public ResponseEntity<SubmissionResponse> getSubmissionDetail(
      @PathVariable UUID teamId, @PathVariable Long submissionId) {
    return ResponseEntity.ok(submissionQueryService.getSubmissionDetail(submissionId, teamId));
  }

  /**
   * Admin variant: full feedback for any submission regardless of team, for support/auditing.
   *
   * @param submissionId the submission ID
   */
  @GetMapping("/admin/submissions/{submissionId}")
  public ResponseEntity<SubmissionResponse> getSubmissionDetailForAdmin(
      @PathVariable Long submissionId) {
    return ResponseEntity.ok(submissionQueryService.getSubmissionDetailForAdmin(submissionId));
  }

  @GetMapping("/events/{eventId}/levels/{levelId}/leaderboard")
  public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard(
    @PathVariable UUID eventId,
    @PathVariable Long levelId
  ) {
    return ResponseEntity.ok(leaderboardService.getLeaderboard(eventId, levelId));
  }

  @GetMapping("/events/{eventId}/leaderboard")
  public ResponseEntity<List<LeaderboardEntryResponse>> getEventLeaderboard(
    @PathVariable UUID eventId
  ) {
    return ResponseEntity.ok(leaderboardService.getEventLeaderboard(eventId));
  }

  @GetMapping(value = "/events/{eventId}/leaderboard/update", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter updateEventLeaderboard(@PathVariable UUID eventId) {
    return leaderboardUpdateService.subscribe(eventId);
  }
}
