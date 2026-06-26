package com.hackathon.platform.scoring;

/**
 * Thrown when the solver subprocess fails to produce a usable result: non-zero exit code, an
 * unparsable final line, or it exceeded its time limit.
 */
public class SolverExecutionException extends RuntimeException {

  private final String errorType;

  public SolverExecutionException(String message, String errorType) {
    super(message);
    this.errorType = errorType;
  }

  public SolverExecutionException(String message, String errorType, Throwable cause) {
    super(message, cause);
    this.errorType = errorType;
  }

  public String getErrorType() {
    return errorType;
  }
}
