package com.romi.mogumogu.dto.auth;

import com.romi.mogumogu.enums.UserRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "1") UserRole role,
        @Schema(example = "test") @Size(max = 32) @NotBlank String username,
        @Schema(example = "test@test.com") @Size(max = 255) @NotBlank @Email String email,
        @Schema(example = "123") @Size(max = 64) @NotBlank String password) {
}
