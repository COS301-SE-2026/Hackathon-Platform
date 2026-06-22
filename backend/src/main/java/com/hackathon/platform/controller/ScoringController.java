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
 * <p>Scoring is currently triggered synchronously over HTTP (no queue yet) - call {@code
 * POST /api/scoring/submissions/{id}/score} right after a submission is uploaded via {@code
 * StorageController}, or have an admin call it again to re-score after a solver hotfix.
 */
@RestController
@RequestMapping("/api/scoring")
@RequiredArgsConstructor
public class ScoringController {

  private final ScoringService scoringService;
  private final SubmissionQueryService submissionQueryService;

  /**
   * Triggers scoring for a submission: runs the active solver against the submission's output
   * file and persists score, status and logs. Safe to call again later (e.g. admin-triggered
   * re-scoring after a solver hotfix) - old log entries are preserved, not overwritten.
   *
   * @param submissionId the submission to score
   * @return the updated submission with score/status set
   */
  @PostMapping("/submissions/{submissionId}/score")
  public ResponseEntity<Submission> scoreSubmission(@PathVariable Long submissionId) {
    Submission scored = scoringService.scoreSubmission(submissionId);
    return ResponseEntity.ok(scored);
  }

  
}
