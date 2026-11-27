package com.romi.mogumogu.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Size(max = 255) @NotBlank @Email String email,
        @Size(max = 64) @NotBlank String password) {
}
