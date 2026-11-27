package com.romi.mogumogu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
public class UpdateRestaurantCategoryDto {

    @Size(max = 32)
    @Schema(description = "分類名稱", example = "甜點")
    private String categoryName;

    @Min(1)
    @Schema(description = "群組內顯示排序 ID", example = "1")
    private Integer displayOrderId;
}
