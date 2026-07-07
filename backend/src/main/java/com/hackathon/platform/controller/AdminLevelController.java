package com.hackathon.platform.controller;

import com.hackathon.platform.dto.LevelRequest;
import com.hackathon.platform.model.Level;
import com.hackathon.platform.service.LevelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
public class AdminLevelController {
    private final LevelService levelService;

    public AdminLevelController(LevelService levelService){
        this.levelService = levelService;
    }

    @PostMapping("/api/hackathons/{hackathonId}/levels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Level> createLevel(@PathVariable UUID hackathonId, @RequestBody LevelRequest req){
        return ResponseEntity.ok(levelService.createLevel(hackathonId, req));
    }

    @PutMapping("/api/levels/{levelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Level> updateLevel(@PathVariable short levelId, @RequestBody LevelRequest req){
        return ResponseEntity.ok(levelService.updateLevel(levelId, req));
    }

    @DeleteMapping("/api/levels/{levelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLevel(@PathVariable("levelId") short levelId) {
        levelService.deleteLevel(levelId);
        return ResponseEntity.noContent().build();
    }
}