package com.romi.mogumogu.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantListResponse<T> {
    @Schema(description = "資料列表")
    private List<T> data;

    @Schema(description = "頁碼")
    private Integer page;

    @Schema(description = "每頁筆數")
    private Integer limit;

    @Schema(description = "總筆數")
    private Long total;
}
