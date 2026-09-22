package com.hackathon.platform.service;

import java.util.UUID;

public record ForumUpdatedEvent(UUID eventId, String action, UUID resourceId) {}
