package com.hackathon.platform.ide;

import com.hackathon.platform.model.User;
import com.hackathon.platform.service.CodeWorkspaceService;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceCollaborationService {
  private final CodeWorkspaceService codeService;
  private final IdeContainerManager containerManager;
  private final WorkspaceInitializationService initialService;
  private final WorkspaceFileStore fileStore;
  private final ConcurrentMap<String, AtomicLong> fileV = new ConcurrentHashMap<>();

  public WorkspaceEditBroadcast applyEdit(
      UUID workspaceId, WorkspaceEditMessage message, User user) {
    var workspace = codeService.getWorkspaceForUser(workspaceId, user);
    IdeWorkspaceResources resources =
        IdeWorkspaceResources.forWorkspace(workspace.getWorkspaceId());

    containerManager.startOrReuse(resources);
    initialService.initializeIfNeeded(workspace.getWorkspaceId(), resources);
    String vKey = workspaceId + ":" + message.path();
    AtomicLong version = fileV.computeIfAbsent(vKey, key -> new AtomicLong(0));

    synchronized (version) {
      long currV = version.get();
      if (message.baseVersion() != currV) {
        throw new IllegalStateException(
            "Stale file version, Expected" + currV + " but got: " + message.baseVersion());
      }

      fileStore.writeFile(resources, message.path(), message.content());
      long newVersion = version.incrementAndGet();

      return new WorkspaceEditBroadcast(
          message.path(), message.content(), newVersion, user.getUserId());
    }
  }

  public long getVersion(UUID workspaceId, String path, User user) {
    codeService.getWorkspaceForUser(workspaceId, user);
    String vKey = workspaceId + ":" + path;
    return fileV.computeIfAbsent(vKey, key -> new AtomicLong(0)).get();
  }
}
