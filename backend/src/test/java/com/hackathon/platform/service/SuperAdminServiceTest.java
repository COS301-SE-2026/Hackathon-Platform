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

}