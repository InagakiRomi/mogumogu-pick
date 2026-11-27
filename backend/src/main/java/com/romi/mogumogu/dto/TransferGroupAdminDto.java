package com.romi.mogumogu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class TransferGroupAdminDto {
    @NotNull
    @Positive
    @Schema(description = "要移轉管理權的目標成員 userId", example = "15")
    private Integer targetUserId;
}
