package com.hackathon.platform.repository;

import com.hackathon.platform.model.Event;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, UUID> {
  @Query("SELECT e FROM Event e WHERE e.createdByUserId = :userId")
  List<Event> fetchAllByAdmin(@Param("userId") UUID userId);

  List<Event> findByVisibilityAndStatusIn(String visibility, List<String> statuses);

  @Query("SELECT e.hackathon.hackathonId FROM Event e WHERE e.eventId = :eventId")
  Optional<UUID> findHackathonIdByEventId(@Param("eventId") UUID eventId);

  @Query(
      """
        SELECT e FROM Event e
        WHERE e.status = 'ACTIVE'
        AND EXISTS (
        SELECT t FROM Team t
        WHERE t.eventId = e.eventId
        AND t.status = 'ACTIVE'
        AND EXISTS (
            SELECT tm FROM TeamMember tm
            WHERE tm.teamId = t.teamId
            AND tm.userId = :userId
            AND tm.status = 'APPROVED'
            )
        )
        """)
  List<Event> findUserActiveEvents(@Param("userId") UUID userId);

  @Query(
      """
    SELECT e FROM Event e
    WHERE e.status IN ('COMPLETED')
    AND EXISTS(
        SELECT t FROM Team t
        WHERE t.eventId = e.eventId
        AND EXISTS(
          SELECT tm FROM TeamMember tm
          WHERE tm.teamId = t.teamId
          AND tm.userId = :userId
          AND tm.status IN ('APPROVED', 'ACTIVE')
        )
    )
""")
  List<Event> findUserCompletedEvents(@Param("userId") UUID userId);

  
}
