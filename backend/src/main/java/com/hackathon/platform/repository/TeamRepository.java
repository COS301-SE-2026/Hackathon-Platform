package com.hackathon.platform.repository;

import com.hackathon.platform.model.Team;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, UUID> {
  boolean existsByEventIdAndTeamName(UUID eventId, String teamName);
}
