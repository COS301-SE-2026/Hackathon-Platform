package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdeWorkspaceResourcesTest {

  @Test
  void forWorkspace_buildsNamesFromWorkspaceId() {
    UUID id = UUID.randomUUID();

    IdeWorkspaceResources res = IdeWorkspaceResources.forWorkspace(id);

    assertThat(res.workspaceId()).isEqualTo(id);
    assertThat(res.containerName()).isEqualTo("hackathon-ide-" + id);
    assertThat(res.codeVolumeName()).isEqualTo("hackathon-code-" + id);
  }

  @Test
  void forWorkspace_throws_whenIdNull() {
    assertThatThrownBy(() -> IdeWorkspaceResources.forWorkspace(null))
        .isInstanceOf(NullPointerException.class);
  }
}
