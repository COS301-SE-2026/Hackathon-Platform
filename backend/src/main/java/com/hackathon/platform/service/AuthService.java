package com.hackathon.platform.service;

import com.hackathon.platform.dto.AuthResponse;
import com.hackathon.platform.dto.LoginRequest;
import com.hackathon.platform.dto.RegisterRequest;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.RoleRepository;
import com.hackathon.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles registration, login and tokens */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  /**
   * Registers a new account.
   *
   * @param request the registration details
   * @return AuthReponse
   */
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("An account with this email address already exists");
    }

    Role participantRole =
        roleRepository
            .findByName("PARTICIPANT")
            .orElseThrow(() -> new IllegalStateException("PARTICIPANT role not found in database"));

    User user =
        User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(participantRole)
            .status("ACTIVE")
            .build();

    User saved = userRepository.save(user);
    String token = jwtService.generateToken(saved);

    return buildResponse(saved, token);
  }

  /**
   * Authenticates a user.
   *
   * @param request login details
   * @return AuthResponse
   */
  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid email or password");
    }

    if (!user.isEnabled()) {
      throw new BadCredentialsException("This account has been deactivated");
    }

    String token = jwtService.generateToken(user);
    return buildResponse(user, token);
  }

  /**
   * Returns profile of a user already logged in.
   *
   * @param user
   * @return AuthResponse
   */
  public AuthResponse getMe(User user) {
    String token = jwtService.generateToken(user);
    return buildResponse(user, token);
  }

  /**
   * Builds an AuthResponse from User and JWT.
   *
   * @param user
   * @param token
   * @return
   */
  private AuthResponse buildResponse(User user, String token) {
    return AuthResponse.builder()
        .token(token)
        .userId(user.getUserId())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .email(user.getEmail())
        .role(user.getRole().getName())
        .build();
  }
}
