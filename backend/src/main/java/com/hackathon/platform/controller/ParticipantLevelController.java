package com.hackathon.platform.controller;

import com.hackathon.platform.model.Level;
import com.hackathon.platform.service.LevelService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParticipantLevelController {
  private final LevelService levelService;

  public ParticipantLevelController(LevelService levelService) {
    this.levelService = levelService;
  }

  @GetMapping("/api/hackathons/{hackathonId}/levels")
  public ResponseEntity<List<Level>> getLevelsForHackathon(
      @PathVariable("hackathonId") UUID hackathonId) {
    return ResponseEntity.ok(levelService.getLevelByHackathonId(hackathonId));
  }

  @GetMapping("/api/levels/{levelId}")
  public ResponseEntity<Level> getLevel(@PathVariable("levelId") short levelId) {
    return ResponseEntity.ok(levelService.getLevelById(levelId));
  }
}
