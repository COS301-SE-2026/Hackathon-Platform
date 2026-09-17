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
  @Mock private EmailVerificationService veriService;

  @InjectMocks private AuthService authService;

  private Role participantRole;
  private User verifiedUser;

  @BeforeEach
  void setUp() {
    participantRole = Role.builder().roleId(2).name("PARTICIPANT").build();

    verifiedUser =
        User.builder()
            .userId(UUID.randomUUID())
            .firstName("Jane")
            .lastName("Doe")
            .email("jane@example.com")
            .passwordHash("$2a$12$hashedpassword")
            .role(participantRole)
            .status("ACTIVE")
                .emailVerified(true)
                .authProvider("LOCAL")
            .build();
  }

  @Test
  void register_withNewEmail() {
    RegisterRequest request = new RegisterRequest("Jane", "Doe", "jane@example.com", "password123");

    User savedUser = User.builder().userId(UUID.randomUUID()).firstName("Jane").lastName("Doe").email("jane@example.com").passwordHash("$2a$12$hashedpassword").role(participantRole).status("ACTIVE").emailVerified(false).authProvider("LOCAL").build();
    when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
    when(roleRepository.findByName("PARTICIPANT")).thenReturn(Optional.of(participantRole));
    when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedpassword");
    when(userRepository.save(any(User.class))).thenReturn(verifiedUser);

    AuthResponse response = authService.register(request);

    assertThat(response.getToken()).isNull();
    assertThat(response.getEmail()).isEqualTo("jane@example.com");
    assertThat(response.getRole()).isEqualTo("PARTICIPANT");
    assertThat(response.isEmailVerified()).isFalse();
    assertThat(response.getMsg()).contains("verification");
    verify(userRepository).save(any(User.class));
    verify(veriService).sendVerificationEmail(savedUser);
    verify(jwtService, never()).generateToken(any(User.class));
  }

  @Test
  void register_withExistingEmail_throwsIllegalArgumentException() {
    RegisterRequest request = new RegisterRequest("Jane", "Doe", "jane@example.com", "password123");

    when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
    verify(userRepository, never()).save(any());
    verify(veriService, never()).sendVerificationEmail(any(User.class));
  }

  @Test
  void login_withCorrectCredentials_returnsAuthResponse() {
    LoginRequest request = new LoginRequest("jane@example.com", "password123");

    when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(verifiedUser));
    when(passwordEncoder.matches("password123", verifiedUser.getPasswordHash())).thenReturn(true);
    when(jwtService.generateToken(verifiedUser)).thenReturn("mock.jwt.token");

    AuthResponse response = authService.login(request);

    assertThat(response.getToken()).isEqualTo("mock.jwt.token");
    assertThat(response.getEmail()).isEqualTo("jane@example.com");
    assertThat(response.isEmailVerified()).isTrue();
  }

  @Test
  void login_withUnknownEmail_throwsBadCredentialsException() {
    LoginRequest request = new LoginRequest("nobody@example.com", "password123");

    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void login_withUnverifiedEmail_throwsBadCredentialsException() {
    User unverifiedUser = User.builder().userId(UUID.randomUUID()).firstName("Jane").lastName("Doe").email("jane@example.com").passwordHash("$2a$12$hashedpassword").role(participantRole).status("ACTIVE").emailVerified(false).authProvider("LOCAL").build();

    LoginRequest request = new LoginRequest("jane@example.com", "password123");

    when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(unverifiedUser));
    when(passwordEncoder.matches("password123", unverifiedUser.getPasswordHash())).thenReturn(true);

    assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class).hasMessageContaining("verify your email");
    verify(jwtService, never()).generateToken(any(User.class));
  }

  @Test
  void login_withWrongPassword_throwsBadCredentialsException() {
    LoginRequest request = new LoginRequest("jane@example.com", "wrongpassword");

    when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(verifiedUser));
    when(passwordEncoder.matches("wrongpassword", verifiedUser.getPasswordHash()))
        .thenReturn(false);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(BadCredentialsException.class);
    verify(jwtService, never()).generateToken(any(User.class));
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
                .emailVerified(true)
                .authProvider("LOCAL")
            .build();

    LoginRequest request = new LoginRequest("jane@example.com", "password123");

    when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(inactiveUser));
    when(passwordEncoder.matches("password123", inactiveUser.getPasswordHash())).thenReturn(true);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("deactivated");
  }

  @Test
  void verifyEmailWithToken_returnsJwt() throws Exception{
    String rawToken = "raw-token";
    User unverifiedUser = User.builder().userId(UUID.randomUUID()).firstName("Jane").lastName("Doe").email("jane@example.com").passwordHash("$2a$12$hashedpassword").role(participantRole).status("ACTIVE").emailVerified(false).authProvider("LOCAL").build();

    when(veriService.verify(rawToken)).thenReturn(unverifiedUser);
    when(jwtService.generateToken(unverifiedUser)).thenReturn("verified.jwt.token");
    AuthResponse resp = authService.verifyEmail(rawToken);

    assertThat(resp.getToken()).isEqualTo("verified.jwt.token");
    assertThat(resp.getEmail()).isEqualTo("jane@example.com");
    assertThat(resp.getMsg()).contains("verification");
  }

  @Test
  void resendVerificationEmailUnverifiedUser() throws Exception {
    User unverifiedUser = User.builder().userId(UUID.randomUUID()).firstName("Jane").lastName("Doe").email("jane@example.com").passwordHash("$2a$12$hashedpassword").role(participantRole).status("ACTIVE").emailVerified(false).authProvider("LOCAL").build();
    when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(unverifiedUser));
    authService.resendVerificationEmail("jane@example.com");
    verify(veriService).sendVerificationEmail(unverifiedUser);
  }

  @Test
  void resendVerificationEmailVerified() throws Exception{
    User verified = User.builder().userId(UUID.randomUUID()).email("jane@example.com").firstName("Jane").emailVerified(true).build();
    when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(verified));
    authService.resendVerificationEmail("jave@example.com");
    verify(veriService, never()).sendVerificationEmail(any(User.class));
  }
}
