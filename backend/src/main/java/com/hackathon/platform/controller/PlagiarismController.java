package com.hackathon.platform.controller;

import com.hackathon.platform.dto.PlagiarismDiffResponse;
import com.hackathon.platform.dto.PlagiarismRunRequest;
import com.hackathon.platform.dto.SubmissionSimilarityResponse;
import com.hackathon.platform.model.PlagiarismRun;
import com.hackathon.platform.model.User;
import com.hackathon.platform.plagiarism.PlagiarismCheckService;
import com.hackathon.platform.plagiarism.PlagiarismProperties;
import com.hackathon.platform.plagiarism.queue.PlagiarismJobProducer;
import com.hackathon.platform.repository.PlagiarismRunRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only endpoints for the batch plagiarism similarity check. This is meant to
 * be run once a level (or the event) has wrapped up - typically against the top N teams on the
 * leaderboard
 */
@RestController
@RequiredArgsConstructor
public class PlagiarismController {

  private final PlagiarismJobProducer producer;
  private final PlagiarismRunRepository runRepo;
  private final PlagiarismCheckService checkService;
  private final PlagiarismProperties props;

  /**Starts async batch run. Returns immediately with runid to poll */
  @PostMapping("/api/admin/events/{eventId}/plagiarism/runs")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Long>> triggerRun(
    @PathVariable UUID eventId,
    @RequestBody(required = false) PlagiarismRunRequest request,
    @AuthenticationPrincipal User user
  ) {
   Short levelId = request != null ? request.levelId() : null;
   int topN =
        (request != null && request.topN() != null) ? request.topN() : props.getDefaultTopN();
    Long runId = producer.enqueue(eventId, levelId, topN, user.getUserId());
    return ResponseEntity.accepted().body(Map.of("runId", runId));

  }

  /**Poll run status plus summary counts */
  @GetMapping("/api/admin/events/{eventId}/plagiarism/runs")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<PlagiarismRun>> listRuns(@PathVariable UUID eventId) {
    return ResponseEntity.ok(runRepo.findByEventIdOrderByRequestedAtDesc(eventId));

  }

  /**Similarity matrix data for a level (or event if level left out): score per pair */
  @GetMapping("/api/admin/events/{eventId}/plagiarism/pairs")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<SubmissionSimilarityResponse>> getPairs(
    @PathVariable UUID eventId,
    @RequestParam(required = false) Short levelId,
    @RequestParam(defaultValue = "false") boolean onlyFlagged
  ) {
   return ResponseEntity.ok(checkService.getResults(eventId, levelId, onlyFlagged));
  }

  /** Side-by-side normalized-AST/token diff for one flagged pair
   * Highlights which structural fragments matched
   */
  @GetMapping("/api/admin/plagiarism/diff")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PlagiarismDiffResponse> getDiff(
    @RequestParam Long submissionIdA, @RequestParam Long submissionIdB
  ) {
   return ResponseEntity.ok(checkService.getDiff(submissionIdA, submissionIdB));
  }


}