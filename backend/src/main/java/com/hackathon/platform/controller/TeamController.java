package com.hackathon.platform.controller;

import com.hackathon.platform.dto.CreateTeamRequest;
import com.hackathon.platform.dto.TeamResponse;
import com.hackathon.platform.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request,
                                                   Principal principal) {
        UUID currentUserId = UUID.fromString(principal.getName());
        TeamResponse response = teamService.createTeam(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}