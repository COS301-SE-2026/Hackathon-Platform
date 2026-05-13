package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;

public class TeamResponse {

    private UUID teamId;
    private String teamName;
    private UUID eventId;
    private UUID createdByUserId;
    private Instant createdAt;
    private String status;

    public TeamResponse() {}

    public TeamResponse(UUID teamId, String teamName, UUID eventId, UUID createdByUserId, Instant createdAt, String status) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.eventId = eventId;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.status = status;
    }


    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}