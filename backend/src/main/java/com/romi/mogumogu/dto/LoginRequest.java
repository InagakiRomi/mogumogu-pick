package com.romi.mogumogu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @Schema(description = "電子郵件", example = "groupadmin@test.com")
    @NotBlank
    @Email
    private String email;

    @Schema(description = "密碼", example = "123")
    @NotBlank
    private String password;
}
