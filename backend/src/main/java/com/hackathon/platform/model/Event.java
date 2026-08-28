package com.hackathon.platform.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "events", schema = "public")
public class Event {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "event_id", updatable = false, nullable = false)
  private UUID eventId;

  @Getter
  @Setter
  @Column(name = "hackathon_id")
  private UUID hackathon;

  @Column(name = "created_by_user_id", nullable = false)
  private UUID createdByUserId;

  @Column(length = 100, nullable = false)
  private String name;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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

  @Column(name = "banner_storage_key", columnDefinition = "TEXT", nullable = true)
  private String bannerStorageKey;

  @Column(name = "logo_storage_key", columnDefinition = "TEXT", nullable = true)
  private String logoStorageKey;

  public String getBannerStorageKey() {
    return bannerStorageKey;
  }

  public void setBannerStorageKey(String bannerStorageKey) {
    this.bannerStorageKey = bannerStorageKey;
  }

  public String getLogoStorageKey() {
    return logoStorageKey;
  }

  public void setLogoStorageKey(String logoStorageKey) {
    this.logoStorageKey = logoStorageKey;
  }

  @Column(name = "scoring_paused", nullable = false)
  private boolean scoringPaused = false;

  @Getter
  @Setter
  @Column(name = "is_in_person", nullable = false)
  private boolean inPerson = false;

  @Getter
  @Setter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "allowed_technologies", columnDefinition = "jsonb", nullable = false)
  private List<String> allowedTech = new ArrayList<>();

  @Getter
  @Setter
  @Column(name = "rules", nullable = false)
  private String rules;

  @Getter
  @Setter
  @Column(name = "tagline", length = 255)
  private String tagline;

  @Getter
  @Setter
  @Column(name = "first_place_prize", precision = 12, scale = 2)
  private BigDecimal firstPlacePrize = BigDecimal.ZERO;

  @Getter
  @Setter
  @Column(name = "second_place_prize", precision = 12, scale = 2)
  private BigDecimal secondPlacePrize = BigDecimal.ZERO;

  @Getter
  @Setter
  @Column(name = "third_place_prize", precision = 12, scale = 2)
  private BigDecimal thirdPlacePrize = BigDecimal.ZERO;

  @Getter
  @Setter
  @Column(name = "total_prize_pool", precision = 12, scale = 2)
  private BigDecimal totalPrizePool = BigDecimal.ZERO;

  @Getter
  @Setter
  @Column(name = "leaderboard_freeze_duration")
  private OffsetDateTime leaderboardFreezeDuration;

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

  @Transient
  public OffsetDateTime getEndDateTime() {
    return startDateTime == null ? null : startDateTime.plusSeconds(duration);
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

  public boolean getScoringPaused() {
    return scoringPaused;
  }

  public void setScoringPaused(boolean scoringPaused) {
    this.scoringPaused = scoringPaused;
  }
}
