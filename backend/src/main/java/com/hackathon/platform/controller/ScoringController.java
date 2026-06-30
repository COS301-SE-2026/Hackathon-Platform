package com.hackathon.platform.controller;

import com.hackathon.platform.dto.SubmissionResponse;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.scoring.ScoringService;
import com.hackathon.platform.scoring.SubmissionQueryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

  /**
   * Triggers scoring for a submission: runs the active solver against the submission's output file
   * and persists score, status and logs. Safe to call again later (e.g. admin-triggered re-scoring
   * after a solver hotfix) - old log entries are preserved, not overwritten.
   *
   * @param submissionId the submission to score
   * @return the updated submission with score/status set
   */
  @PostMapping("/submissions/{submissionId}/score")
  public ResponseEntity<Submission> scoreSubmission(@PathVariable Long submissionId) {
    Submission scored = scoringService.scoreSubmission(submissionId);
    return ResponseEntity.ok(scored);
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
}
