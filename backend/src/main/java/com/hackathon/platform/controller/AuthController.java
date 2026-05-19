package com.hackathon.platform.controller;

import com.hackathon.platform.dto.AuthResponse;
import com.hackathon.platform.dto.LoginRequest;
import com.hackathon.platform.dto.RegisterRequest;
import com.hackathon.platform.model.User;
import com.hackathon.platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for auth endpoints. Contains: POST /api/auth/register, POST /api/auth/login, GET
 * /api/auth/me
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /**
   * Register a new account.
   *
   * @param request registration details
   * @return 201
   */
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Logs in a user.
   *
   * @param request user details
   * @return 200
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Returns profile of a logged in user.
   *
   * @param currentUser
   * @return 200
   */
  @GetMapping("/me")
  public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal User currentUser) {
    AuthResponse response = authService.getMe(currentUser);
    return ResponseEntity.ok(response);
  }
}
