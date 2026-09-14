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


  
}