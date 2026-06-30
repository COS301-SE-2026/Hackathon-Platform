package com.hackathon.platform.repository;

import com.hackathon.platform.model.ScoringLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoringLogRepository extends JpaRepository<ScoringLog, Long> {

  Optional<ScoringLog> findByTeamIdAndHackathonIdAndLevelId(UUID teamId, UUID hackathonId, Long levelId);

  List<ScoringLog> findByTeamIdAndHackathonId(UUID teamId, UUID hackathonId);
}
