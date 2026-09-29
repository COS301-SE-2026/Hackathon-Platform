package com.hackathon.platform.ide;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class EfsIdeContainerManager implements IdeContainerManager{
    @Override
    public IdeContainerSession startOrReuse(IdeWorkspaceResources resources){
        Objects.requireNonNull(resources, "resources are required");
        return new IdeContainerSession(resources.workspaceId(), 0, "RUNNING");
    }

    @Override
    public void stop(IdeWorkspaceResources resources){}

    @Override
    public void removeContainer(IdeWorkspaceResources resources){}

    @Override
    public void removeVolumes(IdeWorkspaceResources resources){}
}