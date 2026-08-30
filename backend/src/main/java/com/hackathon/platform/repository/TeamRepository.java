package com.hackathon.platform.repository;

import com.hackathon.platform.model.Team;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for Team entities. */
public interface TeamRepository extends JpaRepository<Team, UUID> {

  /** Checks whether a team with the given name already exists in the specified event. */
  boolean existsByEventIdAndTeamName(UUID eventId, String teamName);

  boolean existsByJoinCode(String joinCode);

  Optional<Team> findByJoinCode(String joinCode);

  long countByEventId(UUID eventId);

  /** Count teams for an event with a given status */
  long countByEventIdAndStatus(UUID eventId, String status);

  /** All teams for an event, used to aggregate participant accounts */
  List<Team> findByEventId(UUID eventId);
}
