package com.hackathon.platform.repository;

import com.hackathon.platform.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);
    long countByTeamIdAndStatus(UUID teamId, String status);
    List<TeamMember> findByTeamIdAndStatus(UUID teamId, String status);
}