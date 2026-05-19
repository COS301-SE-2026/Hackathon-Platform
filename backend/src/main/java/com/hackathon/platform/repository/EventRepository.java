package com.hackathon.platform.repository;

import com.hackathon.platform.model.Event;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, UUID> {
  @Query("SELECT e FROM Event e WHERE e.createdByUserId = :userId")
  List<Event> fetchAllByAdmin(@Param("userId") UUID userId);
}
