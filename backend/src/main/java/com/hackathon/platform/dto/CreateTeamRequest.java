package com.hackathon.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateTeamRequest {

    @NotBlank(message = "Team name is required")
    private String teamName;

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    public CreateTeamRequest() {}

    public CreateTeamRequest(String teamName, UUID eventId) {
        this.teamName = teamName;
        this.eventId = eventId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }
}