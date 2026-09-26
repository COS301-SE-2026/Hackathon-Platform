package com.hackathon.platform.dto;

public record WorkspaceRunResponse(boolean success, int exitCode, String output, String error) {}
