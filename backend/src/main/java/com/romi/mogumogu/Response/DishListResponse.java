package com.romi.mogumogu.Response;

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
public class DishListResponse {
    @Schema(description = "餐點列表")
    private List<DishResponse> data;

    @Schema(description = "總筆數", example = "2")
    private Integer total;
}
