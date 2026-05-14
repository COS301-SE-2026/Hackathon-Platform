package com.hackathon.platform.dto;

import java.time.OffsetDateTime;

public class EventRequest {
    private String name;
    private String registrationKey;
    private short teamSizeLimit;
    private OffsetDateTime startDateTime;
    private int duration;
    private String description;
    private String visibility;
    private String status;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getRegistrationKey() {
        return registrationKey;
    }
    public void setRegistrationKey(String registrationKey) {
        this.registrationKey = registrationKey;
    }

    public short getTeamSizeLimit() {
        return teamSizeLimit;
    }
    public void setTeamSizeLimit(short teamSizeLimit) {
        this.teamSizeLimit = teamSizeLimit;
    }

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }
    public void setStartDateTime(OffsetDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public int getDuration() {
        return duration;
    }
    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisibility() {
        return visibility;
    }
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

}