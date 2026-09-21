package com.hackathon.platform.ide;

import java.util.UUID;

public record WorkspaceEditBroadcast(String path, String content, long version, UUID editedByUserId){}