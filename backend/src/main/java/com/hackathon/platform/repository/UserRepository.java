package com.hackathon.platform.repository;

import com.hackathon.platform.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
