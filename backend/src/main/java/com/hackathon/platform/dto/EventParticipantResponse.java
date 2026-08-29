package com.hackathon.platform.dto;

import java.time.Instant;
import java.util.UUID;

public class EventParticipantResponse {

    private UUID userId;
    private String fullName;
    private String email;
    private UUID teamId;
    private String teamName;
    private String teamRole;
    private Instant joinedAt;

    public EventParticipantResponse() {}

    public EventParticipantResponse(
        UUID userId,
        String fullName,
        String email,
        UUID teamId,
        String teamName,
        String teamRole,
        Instant joinedAt
    ) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamRole = teamRole;
        this.joinedAt = joinedAt;
    }

    public UUID getUserId() {
        return userId;

    }

    public void setUserId(UUID userId) {
        this.userId = userId;

    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;

    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamRole() {
        return teamRole;

    }

    public void setTeamRole(String teamRole) {
        this.teamRole = teamRole;

    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}