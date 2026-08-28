package com.hackathon.platform.service;

import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.RoleRepository;
import com.hackathon.platform.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements ApplicationRunner {
  private final UserRepository userRepo;
  private final RoleRepository roleRepo;
  private final PasswordEncoder paswrdEncoder;

  @Value("${platform.superadmin.email:}")
  private String email;

  @Value("${platform.superadmin.password:}")
  private String password;

  @Value("${platform.superadmin.first-name:Platform}")
  private String firstName;

  @Value("${platform.superadmin.last-name:SuperAdmin}")
  private String lastName;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (email == null || email.isBlank() || password == null || password.isBlank()) {
      return;
    }

    String emailFormatted = email.trim().toLowerCase(Locale.ROOT);
    if (userRepo.existsByEmail(emailFormatted)) {
      return;
    }

    Role role =
        roleRepo
            .findByName("SUPERADMIN")
            .orElseThrow(() -> new IllegalStateException("SUPERADMIN role not found"));

    User user =
        User.builder()
            .firstName(firstName.trim())
            .lastName(lastName)
            .email(emailFormatted)
            .passwordHash(paswrdEncoder.encode(password))
            .role(role)
            .status("ACTIVE")
            .build();
    userRepo.save(user);
  }
}
