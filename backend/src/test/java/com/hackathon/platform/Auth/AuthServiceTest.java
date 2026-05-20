package com.hackathon.platform.Auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.*;
import com.hackathon.platform.model.*;
import com.hackathon.platform.repository.*;
import com.hackathon.platform.service.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private JwtService jwtService;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private AuthService authService;

  private Role participantRole;
  private User existingUser;

  @BeforeEach
  void setUp() {
    participantRole = Role.builder().roleId(2).name("PARTICIPANT").build();

    existingUser =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("Jane")
            .lastName("Doe")
            .email("jane@example.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(participantRole)
            .status("ACTIVE")
            .build();
  }

  @Test
  void register_withNewEmail_returnsAuthResponse() {
    RegisterRequest request = new RegisterRequest("Jane", "Doe", "jane@example.com", "password123");

    when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
    when(roleRepository.findByName("PARTICIPANT")).thenReturn(Optional.of(participantRole));
    when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedpassword");
    when(userRepository.save(any(User.class))).thenReturn(existingUser);
    when(jwtService.generateToken(any(User.class))).thenReturn("mock.jwt.token");

    AuthResponse response = authService.register(request);

    assertThat(response.getToken()).isEqualTo("mock.jwt.token");
    assertThat(response.getEmail()).isEqualTo("jane@example.com");
    assertThat(response.getRole()).isEqualTo("PARTICIPANT");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void register_withExistingEmail_throwsIllegalArgumentException() {
    RegisterRequest request = new RegisterRequest("Jane", "Doe", "jane@example.com", "password123");

    when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
    verify(userRepository, never()).save(any());
  }

  @Test
  void login_withCorrectCredentials_returnsAuthResponse() {
    LoginRequest request = new LoginRequest("jane@example.com", "password123");

    when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingUser));
    when(passwordEncoder.matches("password123", existingUser.getPasswordHash())).thenReturn(true);
    when(jwtService.generateToken(existingUser)).thenReturn("mock.jwt.token");

    AuthResponse response = authService.login(request);

    assertThat(response.getToken()).isEqualTo("mock.jwt.token");
    assertThat(response.getEmail()).isEqualTo("jane@example.com");
  }

  @Test
  void login_withUnknownEmail_throwsBadCredentialsException() {
    LoginRequest request = new LoginRequest("nobody@example.com", "password123");

    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void login_withWrongPassword_throwsBadCredentialsException() {
    LoginRequest request = new LoginRequest("jane@example.com", "wrongpassword");

    when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingUser));
    when(passwordEncoder.matches("wrongpassword", existingUser.getPasswordHash()))
        .thenReturn(false);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void login_withInactiveAccount_throwsBadCredentialsException() {
    User inactiveUser =
        User.builder()
            .userId(UUID.randomUUID())
            .email("jane@example.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(participantRole)
            .status("INACTIVE")
            .build();

    LoginRequest request = new LoginRequest("jane@example.com", "password123");

    when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(inactiveUser));
    when(passwordEncoder.matches("password123", inactiveUser.getPasswordHash())).thenReturn(true);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("deactivated");
  }
}
