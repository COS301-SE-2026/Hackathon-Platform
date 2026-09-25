package com.hackathon.platform.ide;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.model.CodeWorkspace;
import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.repository.CodeWorkspaceRepository;
import com.hackathon.platform.repository.LevelFileRepository;
import com.hackathon.platform.service.StorageService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceInitializationService {
  private static final int MAX_FILES = 500;
  private static final long MAX_FILE_SIZE = 1_000_000;
  private static final long MAX_TOTAL_SIZE = 20_000_000;
  private static final String STARTER_FILE_TYPE = "STARTER_ZIP";

  private final CodeWorkspaceRepository workRepo;
  private final LevelFileRepository levelRepo;
  private final StorageService storeService;
  private final AzureBlobConfig blobConfig;
  private final WorkspaceFileStore fileStore;

  private final ConcurrentMap<UUID, Object> workspaceLocks = new ConcurrentHashMap<>();

  public void initializeIfNeeded(UUID workspaceId, IdeWorkspaceResources resources) {
    Object lock = workspaceLocks.computeIfAbsent(workspaceId, id -> new Object());

    synchronized (lock) {
      initializeWorkspace(workspaceId, resources);
    }
  }

  private void initializeWorkspace(UUID workspaceId, IdeWorkspaceResources resources) {
    CodeWorkspace workspace =
        workRepo
            .findById(workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

    if (workspace.getInitializedAt() != null) {
      return;
    }

    if (!fileStore.listFiles(resources).isEmpty()) {
      markInitialized(workspace);
      return;
    }

    List<LevelFile> starterFiles =
        levelRepo.findByLevelIdAndFileType(Long.valueOf(workspace.getLevelId()), STARTER_FILE_TYPE);

    if (starterFiles.isEmpty()) {
      markInitialized(workspace);
      return;
    }

    LevelFile starterFile =
        starterFiles.stream().max(Comparator.comparing(LevelFile::getUpdatedAt)).orElseThrow();

    if (starterFile.getFileName() == null
        || !starterFile.getFileName().toLowerCase().endsWith(".zip")) {
      throw new IllegalStateException("Starter file must be zipped");
    }

    try (InputStream input =
        storeService.download(
            blobConfig.getEventResourcesContainer(), starterFile.getStorageKey())) {
      extractStarterZip(input, resources);
    } catch (IOException e) {
      throw new IllegalStateException("Could not initialize", e);
    }

    markInitialized(workspace);
  }

  private void extractStarterZip(InputStream input, IdeWorkspaceResources resources)
      throws IOException {
    int fileCount = 0;
    int totalSize = 0;

    try (ZipInputStream zip = new ZipInputStream(input)) {
      ZipEntry entry;

      while ((entry = zip.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          zip.closeEntry();
          continue;
        }

        String path = validateZipPath(entry.getName());

        fileCount++;

        if (fileCount > MAX_FILES) {
          throw new IllegalStateException("Starter ZIP contains too many files");
        }

        byte[] contents = readEntry(zip);

        totalSize += contents.length;

        if (totalSize > MAX_TOTAL_SIZE) {
          throw new IllegalStateException("Starter ZIP is too large");
        }

        String text = decodeUtf8(contents, path);
        fileStore.writeFile(resources, path, text);
        zip.closeEntry();
      }
    }
  }

  private byte[] readEntry(ZipInputStream zip) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    long size = 0;
    int read;

    while ((read = zip.read(buffer)) != -1) {
      size += read;
      if (size > MAX_FILE_SIZE) {
        throw new IllegalStateException("Starter file exceeds maximum size");
      }

      output.write(buffer, 0, read);
    }

    return output.toByteArray();
  }

  private String decodeUtf8(byte[] data, String path) {
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(data))
          .toString();
    } catch (CharacterCodingException e) {
      throw new IllegalStateException("Starter ZIP contains a non-text file: " + path, e);
    }
  }

  private String validateZipPath(String path) {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("Starter ZIP contains an invalid path");
    }

    String norm = path.replace('\\', '/');

    while (norm.startsWith("./")) {
      norm = norm.substring(2);
    }

    if (norm.isBlank()
        || norm.startsWith("/")
        || norm.contains("\0")
        || norm.contains("\n")
        || norm.contains("\r")) {
      throw new IllegalArgumentException("Starter ZIP contains an invalid path");
    }

    String[] parts = norm.split("/");

    for (String part : parts) {
      if (part.isBlank() || part.equals(".") || part.equals("..")) {
        throw new IllegalArgumentException("Starter ZIP contains an unsafe path");
      }
    }

    return norm;
  }

  private void markInitialized(CodeWorkspace workspace) {
    workspace.setInitializedAt(Instant.now());
    workRepo.save(workspace);
  }
}
