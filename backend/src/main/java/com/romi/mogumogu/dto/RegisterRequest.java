package com.romi.mogumogu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @Schema(description = "使用者名稱", example = "test")
    @NotBlank
    @Size(max = 64)
    private String username;

    @Schema(description = "電子郵件", example = "test@test.com")
    @NotBlank
    @Email
    private String email;

    @Schema(description = "密碼", example = "123")
    @NotBlank
    @Size(max = 128)
    private String password;

    @Schema(description = "使用者角色", example = "2")
    private Integer role;
}
