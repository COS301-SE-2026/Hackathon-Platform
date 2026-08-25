package com.hackathon.platform.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;

public class EventRequest {
  private String name;
  private String registrationKey;
  private short teamSizeLimit;
  private OffsetDateTime startDateTime;
  private int duration;
  private String description;
  private String visibility;
  private String status;

  @Setter
  @Getter
  private Boolean inPerson;

  @Setter
  @Getter
  private List<String> allowedTech;

  @JsonAlias("eventRules")
  @Getter
  @Setter
  private String rules;

  @Setter
  @Getter
  private String tagline;

  @Setter
  @Getter
  private BigDecimal firstPlacePrize;

  @Setter
  @Getter
  private BigDecimal secondPlacePrize;

  @Setter
  @Getter
  private BigDecimal thirdPlacePrize;

  @Setter
  @Getter
  private BigDecimal totalPrizePool;

  @Setter
  @Getter
  private OffsetDateTime freezeTime;

  @Setter @Getter private UUID hackathonId;

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
