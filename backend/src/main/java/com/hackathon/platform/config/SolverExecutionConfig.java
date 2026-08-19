package com.hackathon.platform.config;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/** Configuration for hardened solver execution: how long a solver is allowed to run, and which */
@Configuration
@Getter
public class SolverExecutionConfig {

  /** Wall-clock seconds a solver run may take before it is forcibly killed and marked FAILED. */
  @Value("${scoring.solver.timeout-seconds:30}")
  private int timeoutSeconds;

  /** Python interpreter used to invoke solver scripts. */
  @Value("${scoring.solver.python-executable:python3}")
  private String pythonExecutable;

  /** Root scratch directory each scoring run gets an isolated subdirectory under. */
  @Value("${scoring.solver.workdir:/tmp/hackathon-scoring}")
  private String workDir;

  /** Max bytes of stdout/stderr captured from the solver, to avoid unbounded memory use. */
  @Value("${scoring.solver.max-output-bytes:1048576}")
  private int maxOutputBytes;

  /** Deny-by-default allow-list of environment variable names passed into solver process */
  @Value("${scoring.solver.allowed-env-keys:PATH,HOME,LANG,LC_ALL,SystemRoot}")
  private String allowedEnvKeysRaw = "PATH,HOME,LANG,LC_ALL,SystemRoot";

  /** Max address space the solver may allocate */
  @Value("${scoring.solver.memory-limit-mb:512}")
  private int memoryLimitMb = 512;

  /** Max CPU time in seconds that the solver process may consume*/
  @Value("${scoring.solver.cpu-limit-seconds:30}")
  private int cpuLimitSeconds = 30;

  public List<String> getAllowedEnvKeys(){
    return Arrays.stream(allowedEnvKeysRaw.split(","))
        .map(String::strip)
        .filter(s -> !s.isEmpty())
        .toList();
  }

}
