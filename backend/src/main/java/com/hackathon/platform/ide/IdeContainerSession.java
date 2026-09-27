package com.hackathon.platform.ide;

import java.util.UUID;

public record IdeContainerSession(UUID workspaceId, int hostPort, String status) {}
