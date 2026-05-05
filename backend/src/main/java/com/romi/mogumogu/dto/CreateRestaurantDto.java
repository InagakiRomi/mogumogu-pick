package com.romi.mogumogu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateRestaurantDto {

    @NotNull
    @Schema(description = "所屬群組 ID", example = "1")
    private Integer groupId;

    @NotNull
    @Schema(description = "餐廳分類 ID（須屬於同一群組）", example = "1")
    private Integer categoryId;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "餐廳名稱", example = "壽司郎")
    private String restaurantName;

    @Size(max = 512)
    @Schema(description = "備註", example = "可訂位")
    private String note;

    @Size(max = 512)
    @Schema(description = "圖片網址", example = "https://example.com/restaurant.jpg")
    private String imageUrl;
}
