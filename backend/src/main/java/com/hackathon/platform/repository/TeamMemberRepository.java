package com.hackathon.platform.repository;

import com.hackathon.platform.model.TeamMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  @Query(
      "SELECT tm FROM TeamMember tm JOIN Team t ON tm.teamId = t.teamId WHERE tm.userId = :userId AND tm.status = :status AND t.eventId = :eventId")
  List<TeamMember> findByUserIdAndStatusAndEventId(
      @Param("userId") UUID userId, @Param("status") String status, @Param("eventId") UUID eventId);
  /** Count memebers with a given status across a set of teams */
  long countByTeamIdInAndStatus(List<UUID> teamIds, String status);
}
