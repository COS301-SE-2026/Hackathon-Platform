package com.hackathon.platform.scoring;

/**
 * Thrown when the solver subprocess fails to produce a usable result: non-zero exit code, an
 * unparsable final line, or it exceeded its time limit.
 */
public class SolverExecutionException extends RuntimeException {

  
}
