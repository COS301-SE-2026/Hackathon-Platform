package com.hackathon.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entity representing a single log entry produced while scoring a submission. Maps to the
 * existing 'scoringlogs' table. A submission can have many log entries.
 */
@Entity
@Table(name = "scoringlogs", schema = "public")
public class ScoringLog {

 
}
