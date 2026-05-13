package com.hackathon.platform.repository;

import com.hackathon.platform.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
}