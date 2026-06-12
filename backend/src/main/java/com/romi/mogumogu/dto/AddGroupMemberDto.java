package com.romi.mogumogu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddGroupMemberDto {
    @NotBlank
    @Email
    @Schema(description = "要加入群組的使用者電子郵件", example = "member@example.com")
    private String email;
}
