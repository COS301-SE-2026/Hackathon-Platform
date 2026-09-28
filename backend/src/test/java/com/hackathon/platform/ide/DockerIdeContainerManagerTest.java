package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** The rest of this class shells out to docker, so only the null guard is unit tested. */
class DockerIdeContainerManagerTest {

  @Test
  void startOrReuse_throws_whenResourcesNull() {
    assertThatThrownBy(() -> new DockerIdeContainerManager().startOrReuse(null))
        .isInstanceOf(NullPointerException.class);
  }
}
