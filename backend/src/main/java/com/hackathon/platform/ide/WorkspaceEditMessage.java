package com.hackathon.platform.ide;

public record WorkspaceEditMessage(String path, String content, long baseVersion){}