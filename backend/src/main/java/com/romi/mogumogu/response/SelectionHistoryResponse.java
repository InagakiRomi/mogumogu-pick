package com.romi.mogumogu.response;

import com.romi.mogumogu.constant.DateTimePatternConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectionHistoryResponse {
    @Schema(description = "歷史紀錄 ID")
    private Integer historyId;

    @Schema(description = "餐廳 ID")
    private Integer restaurantId;

    @Schema(description = "餐廳名稱")
    private String restaurantName;

    @Schema(description = "分類名稱")
    private String category;

    @Schema(description = "選擇時間", pattern = DateTimePatternConstants.STANDARD_DATE_TIME)
    private Date selectedAt;
}
