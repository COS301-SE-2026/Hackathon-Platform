package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Only covers input validation, which happens before any docker command is run. */
class DockerWorkspaceFileStoreTest {
  private final DockerWorkspaceFileStore store = new DockerWorkspaceFileStore();
  private final IdeWorkspaceResources resources =
      IdeWorkspaceResources.forWorkspace(UUID.randomUUID());

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "/etc/passwd", "../secret", "a/../b", "a//b", "a\nb"})
  void readFile_throws_whenPathInvalid(String path) {
    assertThatThrownBy(() -> store.readFile(resources, path))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void writeFile_throws_whenPathInvalid() {
    assertThatThrownBy(() -> store.writeFile(resources, "../secret", "x"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void writeFile_throws_whenContentNull() {
    assertThatThrownBy(() -> store.writeFile(resources, "Main.java", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("content");
  }

  @Test
  void writeFile_throws_whenContentTooLarge() {
    String tooBig = "a".repeat(1_000_001);

    assertThatThrownBy(() -> store.writeFile(resources, "Main.java", tooBig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("large");
  }
}
