package com.hackathon.platform.repository;

import com.hackathon.platform.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for User entity */
public interface UserRepository extends JpaRepository<User, UUID> {
  /**
   * Finds user by their email.
   *
   * @param email
   * @return the user
   */
  Optional<User> findByEmail(String email);

  /**
   * Checks if an email is already exists
   *
   * @param email
   * @return true if exists
   */
  boolean existsByEmail(String email);

  @Query(
      """
        SELECT DISTINCT u FROM User u WHERE u.status = 'ACTIVE'
          AND EXISTS (
            SELECT tm FROM TeamMember tm
            WHERE tm.userId = u.userId AND tm.status = 'APPROVED'
            AND EXISTS (
              SELECT t FROM Team t
              WHERE t.teamId = tm.teamId AND t.eventId = :eventId AND t.status = 'ACTIVE'
            )
          )
        """)
  List<User> findActiveParticipantsByEventId(@Param("eventId") UUID eventId);
}
