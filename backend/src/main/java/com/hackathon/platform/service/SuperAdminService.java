package com.hackathon.platform.service;

import com.hackathon.platform.dto.AuthResponse;
import com.hackathon.platform.dto.AdminResponse;
import java.util.List;
import com.hackathon.platform.dto.CreateAdminRequest;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.RoleRepository;
import com.hackathon.platform.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuperAdminService {
  private final UserRepository userRepo;
  private final RoleRepository roleRepo;
  private final PasswordEncoder pswrdEnc;

  public List<AdminResponse> getAdmins() {
    return userRepo.findAll().stream()
        .filter(user -> "ADMIN".equals(user.getRole().getName()))
        .map(
            user ->
                AdminResponse.builder()
                    .userId(user.getUserId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .status(user.getStatus())
                    .build())
        .toList();
  }

  @Transactional
  public AuthResponse createAdmin(CreateAdminRequest req) {
    String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
    if (userRepo.existsByEmail(email)) {
      throw new IllegalArgumentException("An account with this email already exists");
    }

    Role admin =
        roleRepo
            .findByName("ADMIN")
            .orElseThrow(() -> new IllegalArgumentException("ADMIN role not found"));
    User usr =
        User.builder()
            .firstName(req.getFirstName().trim())
            .lastName(req.getLastName().trim())
            .email(email)
            .passwordHash(pswrdEnc.encode(req.getPassword()))
            .role(admin)
            .status("ACTIVE")
            .build();

    User save = userRepo.save(usr);
    return AuthResponse.builder()
        .userId(save.getUserId())
        .firstName(save.getFirstName())
        .lastName(save.getLastName())
        .email(save.getEmail())
        .role(save.getRole().getName())
        .build();
  }
}
