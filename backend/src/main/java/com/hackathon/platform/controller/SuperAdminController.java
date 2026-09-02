package com.hackathon.platform.controller;

import com.hackathon.platform.dto.AuthResponse;
import com.hackathon.platform.dto.CreateAdminRequest;
import com.hackathon.platform.service.SuperAdminService;
import com.hackathon.platform.dto.AdminResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class SuperAdminController {
  private final SuperAdminService sAdService;

  @PostMapping("/admin")
  public ResponseEntity<AuthResponse> createAdmin(@Valid @RequestBody CreateAdminRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(sAdService.createAdmin(req));
  }

  @GetMapping("/admins")
  public ResponseEntity<List<AdminResponse>> getAdmins() {
    return ResponseEntity.ok(sAdService.getAdmins());
  }
  
}
