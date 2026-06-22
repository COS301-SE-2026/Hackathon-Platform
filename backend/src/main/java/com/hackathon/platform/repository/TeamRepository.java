package com.hackathon.platform.repository;

import com.hackathon.platform.model.Team;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for Team entities. */
public interface TeamRepository extends JpaRepository<Team, UUID> {

  /** Checks whether a team with the given name already exists in the specified event. */
  boolean existsByEventIdAndTeamName(UUID eventId, String teamName);
}
