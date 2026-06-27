package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="hackathon", schema="public")
public class Hackathon {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hackathon_id", updatable = false, nullable = false)
    private UUID hackathon_id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime created_at;

    @Column(name = "description")
    private String description;


}