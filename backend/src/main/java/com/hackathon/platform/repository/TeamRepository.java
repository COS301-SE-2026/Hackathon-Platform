package com.hackathon.platform.repository;

import com.hackathon.platform.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    boolean existsByEventIdAndTeamName(UUID eventId, String teamName);
}