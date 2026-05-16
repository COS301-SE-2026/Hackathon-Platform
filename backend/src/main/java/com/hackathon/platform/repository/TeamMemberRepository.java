package com.hackathon.platform.repository;

import com.hackathon.platform.model.TeamMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for TeamMember entities. */
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

  /** Find a team membership by team ID and user ID. */
  Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

  /** Count members of a team with a specific status. */
  long countByTeamIdAndStatus(UUID teamId, String status);

  /** List all members of a team with a specific status. */
  List<TeamMember> findByTeamIdAndStatus(UUID teamId, String status);

  /** List all memberships of a user with a specific status. */
  List<TeamMember> findByUserIdAndStatus(UUID userId, String status);
}
