package com.hackathon.platform.repository;

import com.hackathon.platform.model.Hackathon;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HackathonRepository extends JpaRepository<Hackathon, UUID> {
}