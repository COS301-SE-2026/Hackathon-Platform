package com.hackathon.platform.dto;

import java.util.UUID;

public record IdeSessionResponse(UUID workspaceId, String ideUrl, String status) {}
