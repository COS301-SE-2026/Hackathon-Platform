package com.hackathon.platform.service;

import com.hackathon.platform.dto.CreateTeamRequest;
import com.hackathon.platform.dto.TeamMemberResponse;
import com.hackathon.platform.dto.TeamResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.TeamMember;
import com.hackathon.platform.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.UserRepository;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.EventRegistrationRepository;

/** Service for standalone team management operations. */
@Service
public class TeamService {

  private static final int MAX_JOIN_CODE_GENERATION_ATTEMPTS = 5;

  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final UserRepository userRepository;
  private final EventRepository eventRepo;
  private final EventRegistrationRepository eventRegistrationRepository;

  public TeamService(
          TeamRepository teamRepository,
          TeamMemberRepository teamMemberRepository,
          UserRepository userRepository,
          EventRepository eventRepo, EventRegistrationRepository eventRegistrationRepository) {
    this.teamRepository = teamRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.userRepository = userRepository;
    this.eventRepo = eventRepo;
    this.eventRegistrationRepository = eventRegistrationRepository;
  }

  /** Create a new standalone team and add the creator as an approved leader. */
  @Transactional
  public TeamResponse createTeam(CreateTeamRequest request, UUID currentUserId) {
    String teamName = request.getTeamName() == null ? "" : request.getTeamName().trim();

    if (teamName.isBlank()) {
      throw new RuntimeException("Team name is required");
    }

    if(request.getEventId() == null){
      throw new RuntimeException("Event id is required");
    }

    Event event = eventRepo.findById(request.getEventId()).orElseThrow(() -> new RuntimeException("Event not found"));

    assertEventAcceptsRegistrations(event);
    assertUserIsRegisteredForEvent(event.getEventId(), currentUserId);

    if(teamRepository.existsByEventIdAndTeamName(currentUserId, teamName)){
      throw new RuntimeException("Team name is in use, please choose a new team name");
    }

    if(!teamMemberRepository.findByUserIdAndStatusAndEventId(currentUserId, "APPROVED", event.getEventId()).isEmpty()){
      throw new RuntimeException("You're already part of a team for this event");
    }

    Team team = new Team();
    team.setTeamName(teamName);
    team.setCreatedByUserId(currentUserId);
    team.setStatus("ACTIVE");
    team.setEventId(event.getEventId());

    Team svdName = saveTeamRetryingJoinCodeCollissions(team);
    TeamMember member = new TeamMember();
    member.setTeamId(svdName.getTeamId());
    member.setUserId(currentUserId);
    member.setStatus("APPROVED");
    return toTeamResponse(svdName);
  }

  /** Get the authenticated user's approved team, if they have one. */
  public List<TeamResponse> getMyTeams(UUID currentUserId) {
    List<TeamMember> teams = teamMemberRepository.findByUserIdAndStatus(currentUserId, "APPROVED");
    return teams.stream()
        .map(
            m ->
                teamRepository
                    .findById(m.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not fund")))
        .map(this::toTeamResponse)
        .collect(Collectors.toList());
  }

  public Optional<TeamResponse> getMyTeamForEvent(UUID currUser, UUID eventId) {
    List<TeamMember> members =
        teamMemberRepository.findByUserIdAndStatusAndEventId(currUser, "APPROVED", eventId);

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
    joinTeamInternal(team, currentUserId);
  }

  @Transactional
  public void requestToJoinTeamByCode(String joinCode, UUID currUser, String regKey) {
    Team team =
        teamRepository
            .findByJoinCode(normalizeJoinCode(joinCode))
            .orElseThrow(() -> new RuntimeException("No team found for that join code"));
    joinTeamInternal(team, currUser);
  }

  private void joinTeamInternal(Team team, UUID currUser) {
    if (!"ACTIVE".equals(team.getStatus())) {
      throw new RuntimeException("Team isnt active");
    }

    Event event =
        eventRepo
            .findById(team.getEventId())
            .orElseThrow(() -> new RuntimeException("Event does not exist"));
    assertEventAcceptsRegistrations(event);
    assertUserIsRegisteredForEvent(event.getEventId(), currUser);

    if (teamMemberRepository.findByTeamIdAndUserId(team.getTeamId(), currUser).isPresent()) {
      throw new RuntimeException("You already requested or are a member for this team");
    }

    if (!teamMemberRepository
        .findByUserIdAndStatusAndEventId(currUser, "APPROVED", event.getEventId())
        .isEmpty()) {
      throw new RuntimeException(
          "Youre already on a team for this event. Leave that team to join a new team");
    }

    long approvedCount = teamMemberRepository.countByTeamIdAndStatus(team.getTeamId(), "APPROVED");
    if (approvedCount >= event.getTeamSizeLimit()) {
      throw new RuntimeException("Team is full");
    }

    TeamMember member = new TeamMember();
    member.setTeamId(team.getTeamId());
    member.setUserId(currUser);
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
      Event event =
          eventRepo
              .findById(team.getEventId())
              .orElseThrow(() -> new RuntimeException("Event not found"));

      if (!teamMemberRepository
          .findByUserIdAndStatusAndEventId(userIdToApprove, "APPROVED", event.getEventId())
          .isEmpty()) {
        throw new RuntimeException(
            "Youre already an approved member for another team for this event");
      }

      long currSize = teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED");
      if (currSize >= event.getTeamSizeLimit()) {
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
    response.setJoinCode(team.getJoinCode());
    return response;
  }

  private void assertEventAcceptsRegistrations(Event event) {
    if ("COMPLETED".equals(event.getStatus()) || "CANCELLED".equals(event.getStatus())) {
      throw new RuntimeException("This event is no longer accepting registrations");
    }
  }

  private Team saveTeamRetryingJoinCodeCollissions(Team team) {
    for (int attempt = 0; attempt < MAX_JOIN_CODE_GENERATION_ATTEMPTS; attempt++) {
      try {
        return teamRepository.save(team);
      } catch (DataIntegrityViolationException e) {
        team.regenJoinCode();
      }
    }
    throw new RuntimeException("could not generate join code");
  }

  private String normalizeJoinCode(String joinCode) {
    return joinCode == null ? "" : joinCode.trim().toUpperCase();
  }

  private void assertUserIsRegisteredForEvent(UUID eventId, UUID userId){
    if(!eventRegistrationRepository.existsByEventIdAndUserId(eventId, userId)){
      throw new RuntimeException("You need to register for this event first");
    }
  }
}
