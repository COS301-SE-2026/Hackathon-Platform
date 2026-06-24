package com.hackathon.platform.repository;

import com.hackathon.platform.model.Level;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelRepository extends JpaRepository<Level, Short> {

    List<Level> findByEventId(UUID eventId);

    List<Level> findByEventIdOrderByLevelNumberAsc(UUID eventId);
}