package com.hackathon.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAdminRequest {
  @NotBlank
  @Size(min = 2, max = 100)
  private String firstName;

  @NotBlank
  @Size(min = 2, max = 100)
  private String lastName;

  @NotBlank @Email private String email;

  @NotBlank
  @Size(min = 8, max = 100)
  private String password;
}
