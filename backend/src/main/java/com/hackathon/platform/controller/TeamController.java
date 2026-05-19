package com.hackathon.platform.controller;

import com.hackathon.platform.dto.ApproveRequest;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.TeamMember;
import com.hackathon.platform.model.User;
import com.hackathon.platform.dto.CreateTeamRequest;
import com.hackathon.platform.dto.TeamMemberResponse;
import com.hackathon.platform.dto.TeamResponse;
import com.hackathon.platform.service.TeamService;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;

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

/** REST controller for team management endpoints. */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

  private final TeamService teamService;
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;

  /** Constructor for TeamController. */
  public TeamController(
          TeamService teamService,
          TeamRepository teamRepository,
          TeamMemberRepository teamMemberRepository) {
      this.teamService = teamService;
      this.teamRepository = teamRepository;
      this.teamMemberRepository = teamMemberRepository;
    }

  /** create a new team */
  @PostMapping
  public ResponseEntity<TeamResponse> createTeam(
      @Valid @RequestBody CreateTeamRequest request,
      @AuthenticationPrincipal User currentUser) {
    //UUID currentUserId = UUID.fromString(principal.getName());
    TeamResponse response = teamService.createTeam(request, currentUser.getUserId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** request to join a team */
  @PostMapping("/{teamId}/join-requests")
  public ResponseEntity<Void> requestToJoin(@PathVariable UUID teamId,
    @AuthenticationPrincipal User currentUser) {
    //UUID currentUserId = UUID.fromString(principal.getName());
    teamService.requestToJoinTeam(teamId, currentUser.getUserId());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /** approve or reject a pending join request (only team creator can do this) */
  @PutMapping("/{teamId}/join-requests/{userId}")
  public ResponseEntity<Void> approveOrRejectJoinRequest(
      @PathVariable UUID teamId,
      @PathVariable UUID userId,
      @RequestBody ApproveRequest request,
      @AuthenticationPrincipal User currentUser) {
    //UUID currentUserId = UUID.fromString(principal.getName());
    teamService.approveOrRejectJoinRequest(teamId, userId, currentUser.getUserId(), request.isApprove());
    return ResponseEntity.ok().build();
  }

  /** leave a team (current authenticated user) */
  @DeleteMapping("/{teamId}/members")
  public ResponseEntity<Void> leaveTeam(@PathVariable UUID teamId,
    @AuthenticationPrincipal User currentUser) {
    //UUID currentUserId = UUID.fromString(principal.getName());
    teamService.leaveTeam(teamId, currentUser.getUserId());
    return ResponseEntity.noContent().build();
  }

  /** view all approved members of a team */
  @GetMapping("/{teamId}/members")
  public ResponseEntity<List<TeamMemberResponse>> viewMembers(@PathVariable UUID teamId) {
    List<TeamMemberResponse> members = teamService.viewTeamMembers(teamId);
    return ResponseEntity.ok(members);
  }

  @GetMapping("/my-team")
  public ResponseEntity<TeamResponse> getMyTeam(@AuthenticationPrincipal User currentUser) {
      // Find team where user is a member (APPROVED status)
      List<TeamMember> members = teamMemberRepository.findByUserIdAndStatus(currentUser.getUserId(), "APPROVED");
      
      if (members.isEmpty()) {
          return ResponseEntity.noContent().build();
      }
      
      Team team = teamRepository.findById(members.get(0).getTeamId())
          .orElseThrow(() -> new RuntimeException("Team not found"));
      
      TeamResponse response = new TeamResponse();
      response.setTeamId(team.getTeamId());
      response.setTeamName(team.getTeamName());
      response.setEventId(team.getEventId());
      response.setCreatedByUserId(team.getCreatedByUserId());
      response.setCreatedAt(team.getCreatedAt());
      response.setStatus(team.getStatus());
      
      return ResponseEntity.ok(response);
  }
}
