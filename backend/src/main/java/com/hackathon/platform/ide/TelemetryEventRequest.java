package com.hackathon.platform.ide;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record TelemetryEventRequest(
    String eventType, LocalDateTime clientTimestamp, long sequenceNumber, JsonNode payload) {}
