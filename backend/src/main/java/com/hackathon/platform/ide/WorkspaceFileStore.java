package com.hackathon.platform.ide;

import com.hackathon.platform.dto.WorkspaceFileEntry;
import java.util.List;

public interface WorkspaceFileStore {
  List<WorkspaceFileEntry> listFiles(IdeWorkspaceResources resources);

  String readFile(IdeWorkspaceResources resources, String path);

  void writeFile(IdeWorkspaceResources resources, String path, String content);
}
