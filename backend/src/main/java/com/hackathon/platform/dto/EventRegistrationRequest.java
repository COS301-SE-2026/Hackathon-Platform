package com.hackathon.platform.dto;

import lombok.Getter;
import lombok.Setter;

public class EventRegistrationRequest {
  @Getter @Setter private String regKey;

  @Getter @Setter private String dietaryReq;

  @Getter @Setter private String allergies;
}
