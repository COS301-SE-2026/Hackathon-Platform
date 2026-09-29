package com.hackathon.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SolverExecutionConfigTest {

  @Test
  void getAllowedEnvKeys_returnsDefaultKeys() {
    SolverExecutionConfig config = new SolverExecutionConfig();

    assertThat(config.getAllowedEnvKeys())
        .containsExactly("PATH", "HOME", "LANG", "LC_ALL", "SystemRoot");
  }

  @Test
  void getAllowedEnvKeys_trimsWhitespaceAndDropsEmptyEntries() {
    SolverExecutionConfig config = new SolverExecutionConfig();
    ReflectionTestUtils.setField(config, "allowedEnvKeysRaw", " PATH , ,HOME,, LANG ");

    assertThat(config.getAllowedEnvKeys()).containsExactly("PATH", "HOME", "LANG");
  }
}
