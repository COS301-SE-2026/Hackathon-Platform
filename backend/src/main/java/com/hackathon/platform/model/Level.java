package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "levels", schema = "public")
public class Level {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private short id;

    @Column(name ="event_id", nullable = false)
    private UUID eventId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "level_number", nullable = false)
    private short levelNumber;

    @Column(name = "description", nullable = true)
    private String description;

    public Level() {}

    public Level( UUID eventId, String name, short levelNumber) {
        this.eventId = eventId;
        this.name = name;
        this.levelNumber = levelNumber;
    }

    public void setId(short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setLevelNumber(short levelNumber) {
        this.levelNumber = levelNumber;
    }

    public short getLevelNumber() {
        return levelNumber;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}