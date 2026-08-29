package com.hackathon.platform.service;

import com.hackathon.platform.dto.CreateTeamRequest;
import com.hackathon.platform.dto.EventParticipantResponse;
import com.hackathon.platform.dto.TeamMemberResponse;
import com.hackathon.platform.dto.TeamResponse;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.TeamMember;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for standalone team management operations. */
@Service
public class TeamService {

  private static final int TEAM_SIZE_LIMIT = 4;

  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final UserRepository userRepository;

  public TeamService(
      TeamRepository teamRepository,
      TeamMemberRepository teamMemberRepository,
      UserRepository userRepository) {
    this.teamRepository = teamRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.userRepository = userRepository;
  }

  /** Create a new standalone team and add the creator as an approved leader. */
  @Transactional
  public TeamResponse createTeam(CreateTeamRequest request, UUID currentUserId) {
    String teamName = request.getTeamName() == null ? "" : request.getTeamName().trim();

    if (teamName.isBlank()) {
      throw new RuntimeException("Team name is required");
    }

    boolean nameExists =
        teamRepository.findAll().stream()
            .anyMatch(team -> teamName.equalsIgnoreCase(team.getTeamName()));

    if (nameExists) {
      throw new RuntimeException("Team name already exists");
    }

    if (!teamMemberRepository.findByUserIdAndStatus(currentUserId, "APPROVED").isEmpty()) {
      throw new RuntimeException("You are already a member of a team");
    }

    Team team = new Team();
    team.setTeamName(teamName);
    team.setCreatedByUserId(currentUserId);
    team.setStatus("ACTIVE");
    team.setEventId(request.getEventId());

    Team savedTeam = teamRepository.save(team);

    TeamMember member = new TeamMember();
    member.setTeamId(savedTeam.getTeamId());
    member.setUserId(currentUserId);
    member.setStatus("APPROVED");
    teamMemberRepository.save(member);

    return toTeamResponse(savedTeam);
  }

  /** Get the authenticated user's approved team, if they have one. */
  public Optional<TeamResponse> getMyTeam(UUID currentUserId) {
    List<TeamMember> members =
        teamMemberRepository.findByUserIdAndStatus(currentUserId, "APPROVED");

    if (members.isEmpty()) {
      return Optional.empty();
    }

    Team team =
        teamRepository
            .findById(members.get(0).getTeamId())
            .orElseThrow(() -> new RuntimeException("Team not found"));

    return Optional.of(toTeamResponse(team));
  }

  /** Request to join a team by creating a pending membership. */
  @Transactional
  public void requestToJoinTeam(UUID teamId, UUID currentUserId) {
    Team team =
        teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

    if (!"ACTIVE".equals(team.getStatus())) {
      throw new RuntimeException("Team is not active");
    }

    if (teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId).isPresent()) {
      throw new RuntimeException("Already requested or member");
    }

    if (!teamMemberRepository.findByUserIdAndStatus(currentUserId, "APPROVED").isEmpty()) {
      throw new RuntimeException("You are already a member of a team");
    }

    long approvedCount = teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED");
    if (approvedCount >= TEAM_SIZE_LIMIT) {
      throw new RuntimeException("Team is full");
    }

    TeamMember member = new TeamMember();
    member.setTeamId(teamId);
    member.setUserId(currentUserId);
    member.setStatus("PENDING");
    teamMemberRepository.save(member);
  }

  /** Approve or reject a pending join request. Only the team creator may do this. */
  @Transactional
  public void approveOrRejectJoinRequest(
      UUID teamId, UUID userIdToApprove, UUID currentUserId, boolean approve) {
    Team team =
        teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

    if (!team.getCreatedByUserId().equals(currentUserId)) {
      throw new RuntimeException("Only the team creator can approve/reject requests");
    }

    TeamMember pendingRequest =
        teamMemberRepository
            .findByTeamIdAndUserId(teamId, userIdToApprove)
            .orElseThrow(() -> new RuntimeException("Join request not found"));

    if (!"PENDING".equals(pendingRequest.getStatus())) {
      throw new RuntimeException("Request already processed");
    }

    if (approve) {
      if (!teamMemberRepository.findByUserIdAndStatus(userIdToApprove, "APPROVED").isEmpty()) {
        throw new RuntimeException("User is already an approved member of another team");
      }

      long currentSize = teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED");
      if (currentSize >= TEAM_SIZE_LIMIT) {
        throw new RuntimeException("Team is full");
      }

      pendingRequest.setStatus("APPROVED");
    } else {
      pendingRequest.setStatus("REJECTED");
    }

    teamMemberRepository.save(pendingRequest);
  }

  /** Leave a team. Approved members are marked LEFT; pending requests are deleted. */
  @Transactional
  public void leaveTeam(UUID teamId, UUID currentUserId) {
    TeamMember membership =
        teamMemberRepository
            .findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new RuntimeException("User not in team"));

    if ("APPROVED".equals(membership.getStatus())) {
      membership.setStatus("LEFT");
      teamMemberRepository.save(membership);
    } else if ("PENDING".equals(membership.getStatus())) {
      teamMemberRepository.delete(membership);
      return;
    } else {
      throw new RuntimeException("Cannot leave with current status: " + membership.getStatus());
    }

    long approvedCount = teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED");
    if (approvedCount == 0) {
      Team team = teamRepository.findById(teamId).orElseThrow();
      team.setStatus("INACTIVE");
      teamRepository.save(team);
    }
  }

  /** View all approved members of a team. */
  public List<TeamMemberResponse> viewTeamMembers(UUID teamId) {
    teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));
    return toMemberResponses(teamId, "APPROVED");
  }

  /**List every approved participant across all teams for an event for admin use */
  public List<EventParticipantResponse> listEventParticipants(UUID eventId) {
    List<Team> teams = teamRepository.findByEventId(eventId);
    if (teams.isEmpty()) {
      return List.of();
    }

    List<UUID> teamIds = teams.stream().map(Team::getTeamId).collect(Collectors.toList());
    Map<UUID, Team> teamsById =
      teams.stream().collect(Collectors.toMap(Team::getTeamId, team -> team));
    
    List<TeamMember> members = teamMemberRepository.findByTeamIdInAndStatus(teamIds, "APPROVED");

    List<UUID> userIds = members.stream().map(TeamMember::getUserId).collect(Collectors.toList());
    Map<UUID, User> usersById =
      userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getUserId, user -> user));

      return members.stream()
        .map(
          member -> {
            Team team = teamsById.get(member.getTeamId());
            User user = usersById.get(member.getUserId());
            if (team == null || user == null) {
              return null;
            }
            String role =
              member.getUserId().equals(team.getCreatedByUserId()) ? "LEADER" : "MEMBER";
            
            return new EventParticipantResponse(
              user.getUserId(),
              user.getFirstName() + " " + user.getLastName(),
              user.getEmail(),
              team.getTeamId(),
              team.getTeamName(),
              role,
              member.getJoinedAt()
            );
          }
        )
      .filter(response -> response != null)
      .collect(Collectors.toList());
      
  }

  /** View pending join requests. Only the team creator may view them. */
  public List<TeamMemberResponse> viewPendingJoinRequests(UUID teamId, UUID currentUserId) {
    Team team =
        teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

    if (!team.getCreatedByUserId().equals(currentUserId)) {
      throw new RuntimeException("Only the team creator can view join requests");
    }

    return toMemberResponses(teamId, "PENDING");
  }

  private List<TeamMemberResponse> toMemberResponses(UUID teamId, String status) {
    Team team =
        teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));
    UUID creatorId = team.getCreatedByUserId();

    return teamMemberRepository.findByTeamIdAndStatus(teamId, status).stream()
        .map(
            member -> {
              User user =
                  userRepository
                      .findById(member.getUserId())
                      .orElseThrow(() -> new RuntimeException("User not found"));

              TeamMemberResponse response = new TeamMemberResponse();
              response.setUserId(member.getUserId());
              response.setFullName(user.getFirstName() + " " + user.getLastName());
              response.setEmail(user.getEmail());
              response.setJoinedAt(member.getJoinedAt());
              response.setRole(member.getUserId().equals(creatorId) ? "LEADER" : "MEMBER");
              return response;
            })
        .collect(Collectors.toList());
  }

  private TeamResponse toTeamResponse(Team team) {
    TeamResponse response = new TeamResponse();
    response.setTeamId(team.getTeamId());
    response.setTeamName(team.getTeamName());
    response.setEventId(team.getEventId());
    response.setCreatedByUserId(team.getCreatedByUserId());
    response.setCreatedAt(team.getCreatedAt());
    response.setStatus(team.getStatus());
    return response;
  }
}
