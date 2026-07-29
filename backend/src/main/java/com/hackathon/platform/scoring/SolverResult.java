package com.hackathon.platform.scoring;

import java.math.BigDecimal;
import java.util.List;

/**
 * Parsed result of a single solver run. If the solver crashes, times out, or produces output that
 * cannot be parsed into this shape, the platform itself synthesises a FAILED result so the
 * participant still gets a meaningful status and log instead of a stuck submission.
 */
public class SolverResult {

  private final BigDecimal score;
  private final String status;
  private final String errorType;
  private final List<String> messages;

  public SolverResult(BigDecimal score, String status, String errorType, List<String> messages) {
    this.score = score;
    this.status = status;
    this.errorType = errorType;
    this.messages = messages;
  }

  public BigDecimal getScore() {
    return score;
  }

  public String getStatus() {
    return status;
  }

  public String getErrorType() {
    return errorType;
  }

  public List<String> getMessages() {
    return messages;
  }
}
