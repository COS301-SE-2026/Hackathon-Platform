package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.WorkspaceFileEntry;
import com.hackathon.platform.dto.WorkspaceRunResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceSubmissionPackagerTest {
  @Mock private WorkspaceFileStore fileStore;
  private WorkspaceSubmissionPackager packager;
  private final IdeWorkspaceResources resources =
      IdeWorkspaceResources.forWorkspace(UUID.randomUUID());

  @BeforeEach
  void setUp() {
    packager = new WorkspaceSubmissionPackager(fileStore, new ObjectMapper());
  }

  @Test
  void createPackage_zipsFilesSkipsDirectoriesAndKeepsOutput() throws IOException {
    when(fileStore.listFiles(resources))
        .thenReturn(
            List.of(
                new WorkspaceFileEntry("src", "src", true),
                new WorkspaceFileEntry("Main.java", "src/Main.java", false)));
    when(fileStore.readFile(resources, "src/Main.java")).thenReturn("class Main {}");
    WorkspaceRunResponse run = new WorkspaceRunResponse(true, 0, "{\"answer\":42}", "");

    WorkspaceSubmissionPackage pkg = packager.createPackage(resources, run);

    assertThat(new String(pkg.outputJson(), StandardCharsets.UTF_8)).isEqualTo("{\"answer\":42}");
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pkg.sourceZip()))) {
      ZipEntry entry = zip.getNextEntry();
      assertThat(entry.getName()).isEqualTo("src/Main.java");
      assertThat(new String(zip.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("class Main {}");
      assertThat(zip.getNextEntry()).isNull();
    }
  }

  @Test
  void createPackage_throws_whenRunFailed() {
    WorkspaceRunResponse run = new WorkspaceRunResponse(false, 1, "", "compile error");

    assertThatThrownBy(() -> packager.createPackage(resources, run))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Execution failed");
  }

  @Test
  void createPackage_throws_whenOutputBlank() {
    WorkspaceRunResponse run = new WorkspaceRunResponse(true, 0, "  ", "");

    assertThatThrownBy(() -> packager.createPackage(resources, run))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no output");
  }

  @Test
  void createPackage_throws_whenOutputNotJson() {
    WorkspaceRunResponse run = new WorkspaceRunResponse(true, 0, "not json {", "");

    assertThatThrownBy(() -> packager.createPackage(resources, run))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not valid JSON");
  }
}
