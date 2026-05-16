package com.hackathon.platform.controller;

import com.hackathon.platform.dto.ApproveRequest;
import com.hackathon.platform.dto.CreateTeamRequest;
import com.hackathon.platform.dto.TeamMemberResponse;
import com.hackathon.platform.dto.TeamResponse;
import com.hackathon.platform.service.TeamService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for team management endpoints. */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

  private final TeamService teamService;

  /** Constructor for TeamController. */
  public TeamController(TeamService teamService) {
    this.teamService = teamService;
  }

  /** create a new team */
  @PostMapping
  public ResponseEntity<TeamResponse> createTeam(
      @Valid @RequestBody CreateTeamRequest request, Principal principal) {
    UUID currentUserId = UUID.fromString(principal.getName());
    TeamResponse response = teamService.createTeam(request, currentUserId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** request to join a team */
  @PostMapping("/{teamId}/join-requests")
  public ResponseEntity<Void> requestToJoin(@PathVariable UUID teamId, Principal principal) {
    UUID currentUserId = UUID.fromString(principal.getName());
    teamService.requestToJoinTeam(teamId, currentUserId);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /** approve or reject a pending join request (only team creator can do this) */
  @PutMapping("/{teamId}/join-requests/{userId}")
  public ResponseEntity<Void> approveOrRejectJoinRequest(
      @PathVariable UUID teamId,
      @PathVariable UUID userId,
      @RequestBody ApproveRequest request,
      Principal principal) {
    UUID currentUserId = UUID.fromString(principal.getName());
    teamService.approveOrRejectJoinRequest(teamId, userId, currentUserId, request.isApprove());
    return ResponseEntity.ok().build();
  }

  /** leave a team (current authenticated user) */
  @DeleteMapping("/{teamId}/members")
  public ResponseEntity<Void> leaveTeam(@PathVariable UUID teamId, Principal principal) {
    UUID currentUserId = UUID.fromString(principal.getName());
    teamService.leaveTeam(teamId, currentUserId);
    return ResponseEntity.noContent().build();
  }

  /** view all approved members of a team */
  @GetMapping("/{teamId}/members")
  public ResponseEntity<List<TeamMemberResponse>> viewMembers(@PathVariable UUID teamId) {
    List<TeamMemberResponse> members = teamService.viewTeamMembers(teamId);
    return ResponseEntity.ok(members);
  }
}
