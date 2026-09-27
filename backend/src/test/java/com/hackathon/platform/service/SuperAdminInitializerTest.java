package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.RoleRepository;
import com.hackathon.platform.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SuperAdminInitializerTest {
  @Mock private UserRepository userRepo;
  @Mock private RoleRepository roleRepo;
  @Mock private PasswordEncoder paswrdEncoder;

  private SuperAdminInitializer initializer;

  private Role superAdminRole;

  @BeforeEach
  void setUp() {
    initializer = new SuperAdminInitializer(userRepo, roleRepo, paswrdEncoder);
    superAdminRole = Role.builder().roleId(1).name("SUPERADMIN").build();

    ReflectionTestUtils.setField(initializer, "firstName", "Platform");
    ReflectionTestUtils.setField(initializer, "lastName", "SuperAdmin");
  }

  private void setEmailAndPassword(String email, String password) {
    ReflectionTestUtils.setField(initializer, "email", email);
    ReflectionTestUtils.setField(initializer, "password", password);
  }

  @Test
  void run_withValidConfig_createsSuperAdminUser() {

    setEmailAndPassword(" SuperAdmin@Test.com ", "supersecret");

    when(userRepo.existsByEmail("superadmin@test.com")).thenReturn(false);
    when(roleRepo.findByName("SUPERADMIN")).thenReturn(Optional.of(superAdminRole));
    when(paswrdEncoder.encode("supersecret")).thenReturn("encoded-password");

    initializer.run(null);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepo).save(captor.capture());

    User saved = captor.getValue();
    assertThat(saved.getEmail()).isEqualTo("superadmin@test.com");
    assertThat(saved.getFirstName()).isEqualTo("Platform");
    assertThat(saved.getLastName()).isEqualTo("SuperAdmin");
    assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
    assertThat(saved.getRole()).isEqualTo(superAdminRole);
    assertThat(saved.getStatus()).isEqualTo("ACTIVE");
  }

  @Test
  void run_withBlankEmail_doesNothing() {
    setEmailAndPassword("", "supersecret");

    initializer.run(null);

    verify(userRepo, never()).existsByEmail(any());
    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  void run_withNullEmail_doesNothing() {
    setEmailAndPassword(null, "supersecret");

    initializer.run(null);

    verify(userRepo, never()).existsByEmail(any());
    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  void run_withBlankPassword_doesNothing() {
    setEmailAndPassword("superadmin@test.com", "");

    initializer.run(null);

    verify(userRepo, never()).existsByEmail(any());
    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  void run_withNullPassword_doesNothing() {
    setEmailAndPassword("superadmin@test.com", null);

    initializer.run(null);

    verify(userRepo, never()).existsByEmail(any());
    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  void run_whenUserAlreadyExists_doesNotCreateUser() {

    setEmailAndPassword("superadmin@test.com", "supersecret");

    when(userRepo.existsByEmail("superadmin@test.com")).thenReturn(true);

    initializer.run(null);

    verify(userRepo, never()).save(any(User.class));

    verify(roleRepo, never()).findByName(any());
  }

  @Test
  void run_whenSuperAdminRoleMissing_throwsIllegalStateException() {

    setEmailAndPassword("superadmin@test.com", "supersecret");

    when(userRepo.existsByEmail("superadmin@test.com")).thenReturn(false);
    when(roleRepo.findByName("SUPERADMIN")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> initializer.run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SUPERADMIN role not found");

    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  void run_normalizesEmailtoLowercaseAndTrimmed() {
    setEmailAndPassword(" MixedCase@Example.COM ", "supersecret");

    when(userRepo.existsByEmail("mixedcase@example.com")).thenReturn(false);
    when(roleRepo.findByName("SUPERADMIN")).thenReturn(Optional.of(superAdminRole));
    when(paswrdEncoder.encode("supersecret")).thenReturn("encoded-password");

    initializer.run(null);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepo).save(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo("mixedcase@example.com");
  }
}
