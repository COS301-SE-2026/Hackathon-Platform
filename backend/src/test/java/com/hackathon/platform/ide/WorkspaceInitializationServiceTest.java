package com.hackathon.platform.ide;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.dto.WorkspaceFileEntry;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.repository.CodeWorkspaceRepository;
import com.hackathon.platform.repository.LevelFileRepository;
import com.hackathon.platform.service.StorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceInitializationServiceTest {
  @Mock private CodeWorkspaceRepository workRepo;
  @Mock private LevelFileRepository levelRepo;
  @Mock private StorageService storageService;
  @Mock private AzureBlobConfig blobConfig;
  @Mock private WorkspaceFileStore fileStore;
  @Mock private CodeWorkspace workspace;
  private WorkspaceInitializationService service;

  private final UUID workspaceId = UUID.randomUUID();
  private final IdeWorkspaceResources resources = IdeWorkspaceResources.forWorkspace(workspaceId);

  @BeforeEach
  void setUp() {
    service =
        new WorkspaceInitializationService(
            workRepo, levelRepo, storageService, blobConfig, fileStore);
  }

  private void givenWorkspaceExists() {
    when(workRepo.findById(workspaceId)).thenReturn(Optional.of(workspace));
  }

  private void givenStarterZip(byte[] zipBytes) {
    LevelFile starter = new LevelFile(1L, "starter.zip", "starter-key", "STARTER_ZIP");
    givenWorkspaceExists();
    when(workspace.getLevelId()).thenReturn((short) 1);
    when(levelRepo.findByLevelIdAndFileType(1L, "STARTER_ZIP")).thenReturn(List.of(starter));
    when(blobConfig.getEventResourcesContainer()).thenReturn("resources");
    when(storageService.download("resources", "starter-key"))
        .thenReturn(new ByteArrayInputStream(zipBytes));
  }

  private static byte[] zipOf(Map<String, byte[]> files) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      for (Map.Entry<String, byte[]> file : files.entrySet()) {
        zip.putNextEntry(new ZipEntry(file.getKey()));
        zip.write(file.getValue());
        zip.closeEntry();
      }
    }
    return out.toByteArray();
  }

  @Test
  void initializeIfNeeded_throws_whenWorkspaceMissing() {
    when(workRepo.findById(workspaceId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.initializeIfNeeded(workspaceId, resources))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void initializeIfNeeded_doesNothing_whenAlreadyInitialized() {
    givenWorkspaceExists();
    when(workspace.getInitializedAt()).thenReturn(Instant.now());

    service.initializeIfNeeded(workspaceId, resources);

    verify(workRepo, never()).save(any());
    verify(fileStore, never()).writeFile(any(), any(), any());
  }

  @Test
  void initializeIfNeeded_marksInitialized_whenFilesAlreadyExist() {
    givenWorkspaceExists();
    when(fileStore.listFiles(resources))
        .thenReturn(List.of(new WorkspaceFileEntry("Main.java", "Main.java", false)));

    service.initializeIfNeeded(workspaceId, resources);

    verify(workspace).setInitializedAt(any(Instant.class));
    verify(workRepo).save(workspace);
  }

  @Test
  void initializeIfNeeded_marksInitialized_whenNoStarterFile() {
    givenWorkspaceExists();
    when(workspace.getLevelId()).thenReturn((short) 1);
    when(levelRepo.findByLevelIdAndFileType(1L, "STARTER_ZIP")).thenReturn(List.of());

    service.initializeIfNeeded(workspaceId, resources);

    verify(workRepo).save(workspace);
    verify(fileStore, never()).writeFile(any(), any(), any());
  }

  @Test
  void initializeIfNeeded_throws_whenStarterFileIsNotZip() {
    givenWorkspaceExists();
    when(workspace.getLevelId()).thenReturn((short) 1);
    LevelFile starter = new LevelFile(1L, "starter.txt", "key", "STARTER_ZIP");
    when(levelRepo.findByLevelIdAndFileType(1L, "STARTER_ZIP")).thenReturn(List.of(starter));

    assertThatThrownBy(() -> service.initializeIfNeeded(workspaceId, resources))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("zipped");
  }

  @Test
  void initializeIfNeeded_extractsStarterZipIntoWorkspace() throws IOException {
    byte[] zip = zipOf(Map.of("src/Main.java", "class Main {}".getBytes(StandardCharsets.UTF_8)));
    givenStarterZip(zip);

    service.initializeIfNeeded(workspaceId, resources);

    verify(fileStore).writeFile(resources, "src/Main.java", "class Main {}");
    verify(workspace).setInitializedAt(any(Instant.class));
    verify(workRepo).save(workspace);
  }

  @Test
  void initializeIfNeeded_throws_whenZipPathIsUnsafe() throws IOException {
    givenStarterZip(zipOf(Map.of("../evil.txt", "x".getBytes(StandardCharsets.UTF_8))));

    assertThatThrownBy(() -> service.initializeIfNeeded(workspaceId, resources))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unsafe");
    verify(workRepo, never()).save(any());
  }

  @Test
  void initializeIfNeeded_throws_whenZipContainsBinaryFile() throws IOException {
    byte[] notUtf8 = new byte[] {(byte) 0xC3, (byte) 0x28};
    givenStarterZip(zipOf(Map.of("data.bin", notUtf8)));

    assertThatThrownBy(() -> service.initializeIfNeeded(workspaceId, resources))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("non-text");
  }

  @Test
  void initializeIfNeeded_throws_whenZipHasTooManyFiles() throws IOException {
    Map<String, byte[]> files = new LinkedHashMap<>();
    for (int i = 0; i <= 500; i++) {
      files.put("f" + i + ".txt", new byte[] {'a'});
    }
    givenStarterZip(zipOf(files));

    assertThatThrownBy(() -> service.initializeIfNeeded(workspaceId, resources))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("too many files");
  }
}
