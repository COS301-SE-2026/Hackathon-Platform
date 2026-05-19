package com.hackathon.platform.controller;

import com.hackathon.platform.dto.ApproveRequest;
import com.hackathon.platform.dto.CreateTeamRequest;
import com.hackathon.platform.dto.TeamMemberResponse;
import com.hackathon.platform.dto.TeamResponse;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.TeamService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for standalone team management endpoints. */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

  private final TeamService teamService;

  public TeamController(TeamService teamService) {
    this.teamService = teamService;
  }

  /** Create a new standalone team. */
  @PostMapping
  public ResponseEntity<TeamResponse> createTeam(
      @Valid @RequestBody CreateTeamRequest request, @AuthenticationPrincipal User currentUser) {
    TeamResponse response = teamService.createTeam(request, currentUser.getUserId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** Get the authenticated user's approved team, if any. */
  @GetMapping("/my-team")
  public ResponseEntity<TeamResponse> getMyTeam(@AuthenticationPrincipal User currentUser) {
    return teamService
        .getMyTeam(currentUser.getUserId())
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /** Request to join a team. */
  @PostMapping("/{teamId}/join-requests")
  public ResponseEntity<Void> requestToJoin(
      @PathVariable UUID teamId, @AuthenticationPrincipal User currentUser) {
    teamService.requestToJoinTeam(teamId, currentUser.getUserId());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /** View pending join requests for a team. Only the team creator should use this. */
  @GetMapping("/{teamId}/join-requests")
  public ResponseEntity<List<TeamMemberResponse>> viewJoinRequests(
      @PathVariable UUID teamId, @AuthenticationPrincipal User currentUser) {
    List<TeamMemberResponse> requests =
        teamService.viewPendingJoinRequests(teamId, currentUser.getUserId());
    return ResponseEntity.ok(requests);
  }

  /** Approve or reject a pending join request. */
  @PutMapping("/{teamId}/join-requests/{userId}")
  public ResponseEntity<Void> approveOrRejectJoinRequest(
      @PathVariable UUID teamId,
      @PathVariable UUID userId,
      @RequestBody ApproveRequest request,
      @AuthenticationPrincipal User currentUser) {
    teamService.approveOrRejectJoinRequest(teamId, userId, currentUser.getUserId(), request.isApprove());
    return ResponseEntity.ok().build();
  }

  /** Leave a team as the current authenticated user. */
  @DeleteMapping("/{teamId}/members")
  public ResponseEntity<Void> leaveTeam(
      @PathVariable UUID teamId, @AuthenticationPrincipal User currentUser) {
    teamService.leaveTeam(teamId, currentUser.getUserId());
    return ResponseEntity.noContent().build();
  }

  /** View all approved members of a team. */
  @GetMapping("/{teamId}/members")
  public ResponseEntity<List<TeamMemberResponse>> viewMembers(@PathVariable UUID teamId) {
    return ResponseEntity.ok(teamService.viewTeamMembers(teamId));
  }
}
