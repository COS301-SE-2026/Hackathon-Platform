package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** The rest of this class shells out to docker, so only the null guard is unit tested. */
class DockerJavaWorkspaceRunnerTest {

  @Test
  void run_throws_whenResourcesNull() {
    assertThatThrownBy(() -> new DockerJavaWorkspaceRunner().run(null))
        .isInstanceOf(NullPointerException.class);
  }
}
