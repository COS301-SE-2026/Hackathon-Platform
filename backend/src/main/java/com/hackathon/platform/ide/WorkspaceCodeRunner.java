package com.hackathon.platform.ide;

import com.hackathon.platform.dto.WorkspaceRunResponse;

public interface WorkspaceCodeRunner {
  WorkspaceRunResponse run(IdeWorkspaceResources resources);
}
