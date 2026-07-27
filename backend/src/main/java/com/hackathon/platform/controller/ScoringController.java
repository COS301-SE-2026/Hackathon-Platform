package com.hackathon.platform.controller;

import com.hackathon.platform.dto.LeaderboardEntryResponse;
import com.hackathon.platform.dto.ScoringLogResponse;
import com.hackathon.platform.dto.SubmissionResponse;
import com.hackathon.platform.model.User;
import com.hackathon.platform.scoring.LeaderboardService;
import com.hackathon.platform.scoring.LeaderboardUpdateService;
import com.hackathon.platform.scoring.ScoringService;
import com.hackathon.platform.scoring.SubmissionQueryService;
import com.hackathon.platform.scoring.queue.ScoringJobProducer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
   * after a solver hotfix) - it only overwrites this submission's own log, no other submission's
   * log is touched.
   *
   * @param submissionId the submission to score
   * @return the updated submission with score/status set
   */
  @PostMapping("/submissions/{submissionId}/score")
  @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<Map<String, String>> scoreSubmission(@PathVariable Long submissionId) {
    String record = scoringJobProducer.enqueue(submissionId);
    return ResponseEntity.accepted()
        .body(
            Map.of(
                "submissionId",
                String.valueOf(submissionId),
                "status",
                "QUEUED",
                "recordId",
                record != null ? record : ""));
  }

  /**
   * Full submission history for a team across all levels, most recent first. Used by the
   * participant portal's "submission history" view.
   *
   * @param teamId the team UUID
   */
  @GetMapping("/teams/{teamId}/submissions")
  @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
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
  @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<List<SubmissionResponse>> getTeamLevelHistory(
      @PathVariable UUID teamId, @PathVariable short levelId) {
    return ResponseEntity.ok(submissionQueryService.getHistoryForTeamAndLevel(teamId, levelId));
  }

  /**
   * Admin bulk rescore: re-enqueues every submission for every event under a hackathon (e.g. after
   * a solver hotfix)
   *
   * @param hackathonId the hackathon whose submissions should be rescored
   */
  @PostMapping("/admin/hackathons/{hackathonId}/rescore")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> rescoreHackathon(@PathVariable UUID hackathonId) {
    List<String> records = scoringJobProducer.enqueueAllForHackathon(hackathonId);
    return ResponseEntity.accepted()
        .body(
            Map.of(
                "hackathonId",
                hackathonId.toString(),
                "queuedCount",
                records.size(),
                "status",
                "QUEUED"));
  }

  @GetMapping("/admin/recentsubmissions/{limit}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<SubmissionResponse>> getRecentSubmissions(
      @PathVariable int limit, @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(submissionQueryService.getRecentSubmissions(user.getUserId(), limit));
  }

  /**
   * Full feedback for a single submission, including score, status and its scoring log (e.g.
   * malformed output, rule violations, validation failures). Scoped to the owning team so a
   * participant can't view another team's feedback by guessing IDs.
   *
   * @param teamId the team UUID
   * @param submissionId the submission ID
   */
  @GetMapping("/teams/{teamId}/submissions/{submissionId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<SubmissionResponse> getSubmissionDetail(
      @PathVariable UUID teamId, @PathVariable Long submissionId) {
    return ResponseEntity.ok(submissionQueryService.getSubmissionDetail(submissionId, teamId));
  }

  /**
   * Just the scoring log for a single submission (score/status not included). Scoped to the owning
   * team so a participant can't view another team's log by guessing IDs.
   *
   * @param teamId the team UUID
   * @param submissionId the submission ID
   */
  @GetMapping("/teams/{teamId}/submissions/{submissionId}/log")
  @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<ScoringLogResponse> getSubmissionLog(
      @PathVariable UUID teamId, @PathVariable Long submissionId) {
    ScoringLogResponse log =
        submissionQueryService.getScoringLogForSubmission(submissionId, teamId);
    return log != null ? ResponseEntity.ok(log) : ResponseEntity.notFound().build();
  }

  /**
   * Admin variant: full feedback for any submission regardless of team, for support/auditing.
   *
   * @param submissionId the submission ID
   */
  @GetMapping("/admin/submissions/{submissionId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SubmissionResponse> getSubmissionDetailForAdmin(
      @PathVariable Long submissionId) {
    return ResponseEntity.ok(submissionQueryService.getSubmissionDetailForAdmin(submissionId));
  }

  /**
   * Admin variant: just the scoring log for any submission regardless of team.
   *
   * @param submissionId the submission ID
   */
  @GetMapping("/admin/submissions/{submissionId}/log")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ScoringLogResponse> getSubmissionLogForAdmin(
      @PathVariable Long submissionId) {
    ScoringLogResponse log = submissionQueryService.getScoringLogForSubmissionAsAdmin(submissionId);
    return log != null ? ResponseEntity.ok(log) : ResponseEntity.notFound().build();
  }

  @GetMapping("/events/{eventId}/levels/{levelId}/leaderboard")
  @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard(
      @PathVariable UUID eventId, @PathVariable short levelId) {
    return ResponseEntity.ok(leaderboardService.getLeaderboard(eventId, levelId));
  }

  @GetMapping("/events/{eventId}/leaderboard")
  @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<List<LeaderboardEntryResponse>> getEventLeaderboard(
      @PathVariable UUID eventId) {
    return ResponseEntity.ok(leaderboardService.getEventLeaderboard(eventId));
  }

  @GetMapping(
      value = "/events/{eventId}/leaderboard/update",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public SseEmitter updateEventLeaderboard(@PathVariable UUID eventId) {
    return leaderboardUpdateService.subscribe(eventId);
  }
}
