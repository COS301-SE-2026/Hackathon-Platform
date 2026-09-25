package com.hackathon.platform.ide;

public interface IdeContainerManager {
  IdeContainerSession startOrReuse(IdeWorkspaceResources resources);

  void stop(IdeWorkspaceResources resources);

  void removeContainer(IdeWorkspaceResources resources);

  void removeVolumes(IdeWorkspaceResources resources);
}
