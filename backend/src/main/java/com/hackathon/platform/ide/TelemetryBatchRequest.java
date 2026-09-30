package com.hackathon.platform.ide;

import java.util.List;
import java.util.UUID;

public record TelemetryBatchRequest(UUID sessionId, List<TelemetryEventRequest> events) {}
