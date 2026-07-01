package com.hackathon.platform.scoring;

/** Bundles the parsed {@link SolverResult} with the raw process output, for scoring logs. */
public class SolverRunOutcome {

  private final SolverResult result;
  private final String stdout;
  private final String stderr;

  public SolverRunOutcome(SolverResult result, String stdout, String stderr) {
    this.result = result;
    this.stdout = stdout;
    this.stderr = stderr;
  }

  public SolverResult getResult() {
    return result;
  }

  public String getStdout() {
    return stdout;
  }

  public String getStderr() {
    return stderr;
  }
}
