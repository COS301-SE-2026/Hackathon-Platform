package com.hackathon.platform.ide;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceIdeAccessManager implements IdeAccessManager {
    @Override
    public String getIdeUrl(IdeContainerSession session){
        Objects.requireNonNull(session, "IDE session required");
        return "";
    }
}