package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DockerIdeAccessManagerTest {
  private final DockerIdeAccessManager manager =
      new DockerIdeAccessManager("http://localhost:{port}/?folder={workspaceId}");

  @Test
  void getIdeUrl_fillsInPortAndWorkspaceId() {
    UUID id = UUID.randomUUID();

    String url = manager.getIdeUrl(new IdeContainerSession(id, 8080, "RUNNING"));

    assertThat(url).isEqualTo("http://localhost:8080/?folder=" + id);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 65536})
  void getIdeUrl_throws_whenPortInvalid(int port) {
    IdeContainerSession session = new IdeContainerSession(UUID.randomUUID(), port, "RUNNING");

    assertThatThrownBy(() -> manager.getIdeUrl(session))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getIdeUrl_throws_whenSessionNull() {
    assertThatThrownBy(() -> manager.getIdeUrl(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructor_throws_whenTemplateBlank() {
    assertThatThrownBy(() -> new DockerIdeAccessManager(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void constructor_throws_whenTemplateNull() {
    assertThatThrownBy(() -> new DockerIdeAccessManager(null))
        .isInstanceOf(NullPointerException.class);
  }
}
