package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.AdminResponse;
import com.hackathon.platform.dto.AuthResponse;
import com.hackathon.platform.dto.CreateAdminRequest;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.RoleRepository;
import com.hackathon.platform.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SuperAdminServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private RoleRepository roleRepo;
    @Mock private PasswordEncoder pswrdEnc;

    private SuperAdminService superAdminService;

    private Role adminRole;
    private Role participantRole;

    @BeforeEach
    void setUp() {
        superAdminService = new SuperAdminService(userRepo, roleRepo, pswrdEnc);

        adminRole = Role.builder().roleId(1).name("ADMIN").build();
        participantRole = Role.builder().roleId(2).name("PARTICIPANT").build();

    }

    private User buildUser(UUID id, String firstName, String lastName, String email, Role role) {
        return User.builder()
            .userId(id)
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .passwordHash("hashed")
            .role(role)
            .status("ACTIVE")
            .build();

    }

    @Test
    void getAdmins_returnsOnlyUsersWithAdminRole() {
        User admin = buildUser(UUID.randomUUID(), "Jane", "Doe", "jane@test.com", adminRole);
        User participant =
            buildUser(UUID.randomUUID(), "John", "Smith", "john@test.com", participantRole);
        when(userRepo.findAll()).thenReturn(List.of(admin, participant));

        List<AdminResponse> results = superAdminService.getAdmins();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("jane@test.com");
        assertThat(results.get(0).getFirstName()).isEqualTo("Jane");
        assertThat(results.get(0).getStatus()).isEqualTo("ACTIVE");
        verify(userRepo).findAll();

    }

}