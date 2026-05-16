package com.hackathon.platform.repository;

import com.hackathon.platform.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repo for role entity. */
public interface RoleRepository extends JpaRepository<Role, Integer> {

  /**
   * Finds a role by its name
   *
   * @param name of the role
   * @return the role
   */
  Optional<Role> findByName(String name);
}
