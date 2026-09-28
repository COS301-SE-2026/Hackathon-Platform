package com.hackathon.platform.service;

import com.hackathon.platform.dto.AdminCreateTeamRequest;
import com.hackathon.platform.dto.AdminTeamResponse;
import com.hackathon.platform.dto.CreateTeamRequest;
import com.hackathon.platform.dto.EventParticipantResponse;
import com.hackathon.platform.dto.TeamMemberResponse;
import com.hackathon.platform.dto.TeamResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.EventRegistration;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.TeamMember;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
      EventRepository eventRepo,
      EventRegistrationRepository eventRegistrationRepository) {
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

    if (request.getEventId() == null) {
      throw new RuntimeException("Event id is required");
    }

    Event event =
        eventRepo
            .findById(request.getEventId())
            .orElseThrow(() -> new RuntimeException("Event not found"));

    assertEventAcceptsRegistrations(event);
    assertUserIsRegisteredForEvent(event.getEventId(), currentUserId);

    if (teamRepository.existsByEventIdAndTeamName(event.getEventId(), teamName)) {
      throw new RuntimeException("Team name is in use, please choose a new team name");
    }

    if (!teamMemberRepository
        .findByUserIdAndStatusAndEventId(currentUserId, "APPROVED", event.getEventId())
        .isEmpty()) {
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
    teamMemberRepository.save(member);
    return toTeamResponse(svdName);
  }

  @Transactional
  public void createTeamAsAdmin(UUID eventId, AdminCreateTeamRequest request) {

    String teamName = request.getTeamName() == null ? "" : request.getTeamName().trim();

    if (teamName.isBlank()) {
      throw new RuntimeException("Team name is required");
    }

    List<String> memberEmails = request.getMemberEmails();

    if (memberEmails == null || memberEmails.isEmpty()) {
      throw new RuntimeException("At least one team member is required");
    }

    Event event =
        eventRepo.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));

    if (teamRepository.existsByEventIdAndTeamName(eventId, teamName)) {
      throw new RuntimeException("Team name is in use, please choose a new team name");
    }

    if (memberEmails.size() > event.getTeamSizeLimit()) {
      throw new RuntimeException(
          "Team cannot have more than " + event.getTeamSizeLimit() + " members");
    }

    Team team = new Team();
    team.setTeamName(teamName);
    team.setEventId(eventId);
    team.setStatus("ACTIVE");

    User firstMemberUser = null;

    for (String email : memberEmails) {

      String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

      if (normalizedEmail.isBlank()) {
        throw new RuntimeException("Member email is required");
      }

      User user =
          userRepository
              .findByEmail(normalizedEmail)
              .orElseThrow(
                  () -> new RuntimeException("No user found with email: " + normalizedEmail));

      EventRegistration registration =
          eventRegistrationRepository
              .findByEventIdAndUserId(eventId, user.getUserId())
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          "User is not registered for this event: " + normalizedEmail));

      if (registration.isBanned()) {
        throw new RuntimeException("User is banned from the event: " + normalizedEmail);
      }

      if (!teamMemberRepository
          .findByUserIdAndStatusAndEventId(user.getUserId(), "APPROVED", eventId)
          .isEmpty()) {

        throw new RuntimeException(
            "User is already a member of a team for this event: " + normalizedEmail);
      }

      if (firstMemberUser == null) {
        firstMemberUser = user;
      }
    }

    if (firstMemberUser == null) {
      throw new RuntimeException("At least one valid team member is required");
    }

    team.setCreatedByUserId(firstMemberUser.getUserId());

    Team savedTeam = saveTeamRetryingJoinCodeCollissions(team);

    for (String email : memberEmails) {

      String normalizedEmail = email.trim().toLowerCase();

      User user =
          userRepository
              .findByEmail(normalizedEmail)
              .orElseThrow(
                  () -> new RuntimeException("No user found with email: " + normalizedEmail));

      TeamMember member = new TeamMember();
      member.setTeamId(savedTeam.getTeamId());
      member.setUserId(user.getUserId());
      member.setStatus("APPROVED");

      teamMemberRepository.save(member);
    }
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
  public void requestToJoinTeamByCode(String joinCode, UUID currUser) {
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
    Team team =
        teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

    TeamMember membership =
        teamMemberRepository
            .findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new RuntimeException("User not in team"));

    if ("APPROVED".equals(membership.getStatus())) {

      boolean leavingLeader = currentUserId.equals(team.getCreatedByUserId());

      membership.setStatus("LEFT");
      teamMemberRepository.save(membership);

      if (leavingLeader) {
        List<TeamMember> remainingMembers =
            teamMemberRepository.findByTeamIdAndStatus(teamId, "APPROVED");

        if (!remainingMembers.isEmpty()) {
          TeamMember newLeader =
              remainingMembers.stream()
                  .min((first, second) -> first.getJoinedAt().compareTo(second.getJoinedAt()))
                  .orElseThrow();
          team.setCreatedByUserId(newLeader.getUserId());
          teamRepository.save(team);
        }
      }
    } else if ("PENDING".equals(membership.getStatus())) {
      teamMemberRepository.delete(membership);
      return;
    } else {
      throw new RuntimeException("Cannot leave with current status: " + membership.getStatus());
    }

    long approvedCount = teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED");
    if (approvedCount == 0) {
      team.setStatus("INACTIVE");
      teamRepository.save(team);
    }
  }

  /** Remove an approved member from a team as an event administrator. */
  @Transactional
  public void removeTeamMember(UUID teamId, UUID userId) {

    Team team =
        teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

    TeamMember membership =
        teamMemberRepository
            .findByTeamIdAndUserId(teamId, userId)
            .orElseThrow(() -> new RuntimeException("Team member not found"));

    if (!"APPROVED".equals(membership.getStatus())) {
      throw new RuntimeException("Only an approved team member can be removed");
    }

    boolean removingLeader = userId.equals(team.getCreatedByUserId());

    membership.setStatus("LEFT");
    teamMemberRepository.save(membership);

    if (removingLeader) {

      List<TeamMember> remainingMembers =
          teamMemberRepository.findByTeamIdAndStatus(teamId, "APPROVED");

      if (!remainingMembers.isEmpty()) {

        TeamMember newLeader =
            remainingMembers.stream()
                .min((first, second) -> first.getJoinedAt().compareTo(second.getJoinedAt()))
                .orElseThrow();
        team.setCreatedByUserId(newLeader.getUserId());
        teamRepository.save(team);

      } else {
        team.setStatus("INACTIVE");
        teamRepository.save(team);
      }

    } else {

      long approvedCount = teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED");

      if (approvedCount == 0) {
        team.setStatus("INACTIVE");
        teamRepository.save(team);
      }
    }
  }

  /** Add a member to a team as an event administrator. */
  @Transactional
  public void addTeamMemberAsAdmin(UUID eventId, UUID teamId, String email) {

    Team team =
        teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

    if (!team.getEventId().equals(eventId)) {
      throw new RuntimeException("Team does not belong to this event");
    }

    String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

    if (normalizedEmail.isBlank()) {
      throw new RuntimeException("Email is required");
    }

    User user =
        userRepository
            .findByEmail(normalizedEmail)
            .orElseThrow(() -> new RuntimeException("No user found with this email"));

    EventRegistration registration =
        eventRegistrationRepository
            .findByEventIdAndUserId(eventId, user.getUserId())
            .orElseThrow(() -> new RuntimeException("User is not registered for this event"));

    if (registration.isBanned()) {
      throw new RuntimeException("This user is banned from the event");
    }

    if (!teamMemberRepository
        .findByUserIdAndStatusAndEventId(user.getUserId(), "APPROVED", eventId)
        .isEmpty()) {

      throw new RuntimeException("User is already a member of a team for this event");
    }

    Event event =
        eventRepo.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));

    long approvedCount = teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED");

    if (approvedCount >= event.getTeamSizeLimit()) {
      throw new RuntimeException("Team is full");
    }

    Optional<TeamMember> existingMembership =
        teamMemberRepository.findByTeamIdAndUserId(teamId, user.getUserId());

    TeamMember member;

    if (existingMembership.isPresent()) {
      member = existingMembership.get();

      if ("LEFT".equals(member.getStatus())) {
        member.setStatus("APPROVED");
      } else if ("PENDING".equals(member.getStatus())) {
        member.setStatus("APPROVED");
      } else if ("APPROVED".equals(member.getStatus())) {
        throw new RuntimeException("User is already a member of this team");
      } else {
        throw new RuntimeException("User cannot be added to this team");
      }

      member.setJoinedAt(Instant.now());
      teamMemberRepository.save(member);

    } else {
      member = new TeamMember();
      member.setTeamId(teamId);
      member.setUserId(user.getUserId());
      member.setStatus("APPROVED");

      teamMemberRepository.save(member);
    }

    if (team.getCreatedByUserId() == null) {
      team.setCreatedByUserId(user.getUserId());
    }

    if ("INACTIVE".equals(team.getStatus())) {
      team.setStatus("ACTIVE");
    }

    teamRepository.save(team);
  }

  /** Ban a participant from an event, mark them as LEFT, and deactivate an empty team. */
  @Transactional
  public void banParticipant(UUID eventId, UUID userId) {
    EventRegistration registration =
        eventRegistrationRepository
            .findByEventIdAndUserId(eventId, userId)
            .orElseThrow(() -> new RuntimeException("Event registration not found"));

    Optional<TeamMember> membership =
        teamMemberRepository.findByUserIdAndStatusAndEventId(userId, "APPROVED", eventId).stream()
            .findFirst();

    if (membership.isPresent()) {
      registration.setBannedFromTeamId(membership.get().getTeamId());
    } else {
      registration.setBannedFromTeamId(null);
    }

    registration.setBanned(true);
    eventRegistrationRepository.save(registration);

    if (membership.isEmpty()) {
      return;
    }

    TeamMember teamMember = membership.get();
    teamMember.setStatus("LEFT");
    teamMemberRepository.save(teamMember);

    long approvedCount =
        teamMemberRepository.countByTeamIdAndStatus(teamMember.getTeamId(), "APPROVED");

    if (approvedCount == 0) {
      Team team =
          teamRepository
              .findById(teamMember.getTeamId())
              .orElseThrow(() -> new RuntimeException("Team not found"));

      team.setStatus("INACTIVE");
      teamRepository.save(team);
    }
  }

  /** Unban a participant from an event and restore the team they were banned from, if any. */
  @Transactional
  public void unbanParticipant(UUID eventId, UUID userId) {
    EventRegistration registration =
        eventRegistrationRepository
            .findByEventIdAndUserId(eventId, userId)
            .orElseThrow(() -> new RuntimeException("Event registration not found"));

    if (!registration.isBanned()) {
      throw new RuntimeException("Participant is not banned");
    }

    UUID bannedFromTeamId = registration.getBannedFromTeamId();

    registration.setBanned(false);
    registration.setBannedFromTeamId(null);
    eventRegistrationRepository.save(registration);

    if (bannedFromTeamId == null) {
      return;
    }

    Optional<TeamMember> membership =
        teamMemberRepository.findByTeamIdAndUserId(bannedFromTeamId, userId);

    if (membership.isEmpty()) {
      throw new RuntimeException("Previous team membership not found");
    }

    TeamMember teamMember = membership.get();
    teamMember.setStatus("APPROVED");
    teamMemberRepository.save(teamMember);

    Team team =
        teamRepository
            .findById(bannedFromTeamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));

    team.setStatus("ACTIVE");
    teamRepository.save(team);
  }

  public List<AdminTeamResponse> listEventTeams(UUID eventId) {
    eventRepo.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));

    List<Team> teams = teamRepository.findByEventId(eventId);

    if (teams.isEmpty()) {
      return List.of();
    }

    List<UUID> teamIds = teams.stream().map(Team::getTeamId).collect(Collectors.toList());

    List<TeamMember> members = teamMemberRepository.findByTeamIdInAndStatus(teamIds, "APPROVED");

    Map<UUID, List<TeamMember>> membersByTeam =
        members.stream().collect(Collectors.groupingBy(TeamMember::getTeamId));

    Map<UUID, User> usersById =
        userRepository
            .findAllById(
                members.stream().map(TeamMember::getUserId).distinct().collect(Collectors.toList()))
            .stream()
            .collect(Collectors.toMap(User::getUserId, user -> user));

    return teams.stream()
        .map(
            team -> {
              AdminTeamResponse response = new AdminTeamResponse();

              response.setTeamId(team.getTeamId());
              response.setTeamName(team.getTeamName());
              response.setEventId(team.getEventId());
              response.setCreatedByUserId(team.getCreatedByUserId());
              response.setCreatedAt(team.getCreatedAt());
              response.setStatus(team.getStatus());

              List<TeamMemberResponse> teamMembers =
                  membersByTeam.getOrDefault(team.getTeamId(), List.of()).stream()
                      .map(
                          member -> {
                            User user = usersById.get(member.getUserId());
                            if (user == null) {
                              throw new RuntimeException("User not found");
                            }

                            TeamMemberResponse memberResponse = new TeamMemberResponse();

                            memberResponse.setUserId(member.getUserId());
                            memberResponse.setFullName(
                                user.getFirstName() + " " + user.getLastName());
                            memberResponse.setEmail(user.getEmail());
                            memberResponse.setJoinedAt(member.getJoinedAt());
                            memberResponse.setRole(
                                member.getUserId().equals(team.getCreatedByUserId())
                                    ? "LEADER"
                                    : "MEMBER");

                            return memberResponse;
                          })
                      .collect(Collectors.toList());

              response.setMembers(teamMembers);

              return response;
            })
        .collect(Collectors.toList());
  }

  public List<TeamMemberResponse> viewTeamMembers(UUID teamId) {
    teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));
    return toMemberResponses(teamId, "APPROVED");
  }

  public List<EventParticipantResponse> listEventParticipants(UUID eventId) {
    List<EventRegistration> registrations = eventRegistrationRepository.findByEventId(eventId);

    if (registrations.isEmpty()) {
      return List.of();
    }

    List<UUID> userIds =
        registrations.stream()
            .map(EventRegistration::getUserId)
            .distinct()
            .collect(Collectors.toList());

    Map<UUID, User> usersById =
        userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getUserId, user -> user));

    List<Team> teams = teamRepository.findByEventId(eventId);

    Map<UUID, Team> teamsById =
        teams.stream().collect(Collectors.toMap(Team::getTeamId, team -> team));

    List<UUID> teamIds = teams.stream().map(Team::getTeamId).collect(Collectors.toList());

    List<TeamMember> members =
        teamIds.isEmpty()
            ? List.of()
            : teamMemberRepository.findByTeamIdInAndStatus(teamIds, "APPROVED");

    Map<UUID, TeamMember> membershipByUserId =
        members.stream()
            .collect(
                Collectors.toMap(
                    TeamMember::getUserId, member -> member, (existing, replacement) -> existing));

    return registrations.stream()
        .map(
            registration -> {
              User user = usersById.get(registration.getUserId());

              if (user == null) {
                return null;
              }

              TeamMember member =
                  registration.isBanned() ? null : membershipByUserId.get(registration.getUserId());

              UUID teamId = null;
              String teamName = null;
              String teamRole = null;
              java.time.Instant joinedAt = null;

              if (member != null) {
                Team team = teamsById.get(member.getTeamId());

                if (team != null) {
                  teamId = team.getTeamId();
                  teamName = team.getTeamName();

                  teamRole =
                      member.getUserId().equals(team.getCreatedByUserId()) ? "LEADER" : "MEMBER";

                  joinedAt = member.getJoinedAt();
                }
              }

              return new EventParticipantResponse(
                  user.getUserId(),
                  user.getFirstName() + " " + user.getLastName(),
                  user.getEmail(),
                  teamId,
                  teamName,
                  teamRole,
                  joinedAt,
                  registration.isBanned(),
                  registration.getDietaryReq(),
                  registration.getAllergies());
            })
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

  private void assertUserIsRegisteredForEvent(UUID eventId, UUID userId) {
    if (!eventRegistrationRepository.existsByEventIdAndUserId(eventId, userId)) {
      throw new RuntimeException("You need to register for this event first");
    }
  }
}
