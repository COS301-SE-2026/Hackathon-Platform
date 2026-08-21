package com.hackathon.platform.service;

import java.util.UUID;

public record AnnouncementCreatedEvent(UUID eventId, UUID messageId){}