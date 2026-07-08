package com.hackathon.platform.dto;

import lombok.Getter;
import lombok.Setter;

public class LevelRequest {

  @Setter @Getter private String name;

  @Getter @Setter private Short levelNumber;

  @Getter @Setter private String description;
}
