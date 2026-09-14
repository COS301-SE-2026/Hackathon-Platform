package com.hackathon.platform.repository;

import com.hackathon.platform.model.CodeWorkspace;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CodeWorkspaceRepository extends JpaRepository<CodeWorkspace, UUID> {
    Optional<CodeWorkspace> findByEventIdAndTeamIdAndLevelId(UUID eventId, UUID teamId, short levelId);

    @Modifying
    @Transactional
    @Query(value =  """
                        INSERT INTO code_workspaces (event_id, team_id, hackathon_id, level_id, created_by_user_id)
                        VALUES (:eventId, :teamId, :hackathonId, :levelId, :createdByUserId)
                        ON CONFLICT ON CONSTRAINT uq_code_workspaces_event_team_level DO NOTHING
                    """, nativeQuery = true)
                    int createIfMissing(@Param("eventId") UUID eventId, @Param("teamId") UUID teamId, @Param("hackathonId") UUID hackathonId, @Param("levelId") short levelId, @Param("createdByUserId") UUID createdByUserId);
}