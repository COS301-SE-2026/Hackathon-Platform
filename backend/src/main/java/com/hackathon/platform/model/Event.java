package com.hackathon.platform.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import java.util.UUID;
import java.time.OffsetDateTime;

@Entity
@Table(name = "events", schema = "public")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID eventId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "registration_key", nullable = true, length = 50)
    private String registrationKey;

    @Column(name = "team_size_limit", nullable = false)
    private short teamSizeLimit;

    @Column(name = "start_datetime", nullable = false)
    private OffsetDateTime startDateTime;

    @Column(nullable = false)
    private int duration;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String description;

    @Column(nullable = false, length = 20)
    private String visibility;

    @Column(nullable = false, length = 30)
    private String status;

    public UUID getEventId() {
        return eventId;
    }
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }
    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

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