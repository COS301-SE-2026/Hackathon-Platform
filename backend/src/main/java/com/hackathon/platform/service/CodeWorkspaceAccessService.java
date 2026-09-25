package com.hackathon.platform.service;

import com.hackathon.platform.model.Level;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.LevelRepository;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CodeWorkspaceAccessService {
  private final EventRepository eventRepo;
  private final LevelRepository levelRepo;
  private final TeamRepository teamRepo;
  private final TeamMemberRepository teamMemRepo;

  public UUID requireParticipantAccess(UUID eventId, UUID teamId, short levelId, UUID userId) {
    Team team =
        teamRepo.findById(teamId).orElseThrow(() -> new IllegalArgumentException("Team not found"));

    if (!eventId.equals(team.getEventId())) {
      throw new AccessDeniedException("This event is not open to this team");
    }

    boolean member =
        teamMemRepo.findByUserIdAndStatusAndEventId(userId, "APPROVED", eventId).stream()
            .anyMatch(m -> teamId.equals(m.getTeamId()));

    if (!member) {
      throw new AccessDeniedException("You are not appart of this team");
    }

    Level lvl =
        levelRepo
            .findById(levelId)
            .orElseThrow(() -> new IllegalArgumentException("Level could not be found"));

    UUID hackId =
        eventRepo
            .findHackathonIdByEventId(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Hackathon could not be found"));

    if (!hackId.equals(lvl.getHackathonId())) {
      throw new AccessDeniedException("Level does not belong to this event");
    }

    return hackId;
  }
}
