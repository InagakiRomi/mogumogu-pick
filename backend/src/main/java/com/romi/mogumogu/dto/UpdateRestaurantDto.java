package com.romi.mogumogu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRestaurantDto {
    @Min(0)
    @Schema(description = "餐廳分類 ID（須屬於餐廳既有群組）", example = "1")
    private Integer categoryId;

    @Min(0)
    @Schema(description = "群組內顯示排序", example = "1")
    private Integer displayOrder;

    @Min(0)
    @Schema(description = "被選取次數", example = "10")
    private Integer selectedCount;

    @Size(max = 64)
    @Schema(description = "餐廳名稱", example = "和食天國")
    private String restaurantName;

    @Size(max = 512)
    @Schema(description = "備註", example = "可訂位")
    private String note;

    @Size(max = 512)
    @Schema(description = "圖片網址", example = "https://example.com/restaurant.jpg")
    private String imageUrl;

    @Schema(description = "最後被選取時間", example = "2026-05-03 14:58:57")
    private Date lastSelectedAt;
}
