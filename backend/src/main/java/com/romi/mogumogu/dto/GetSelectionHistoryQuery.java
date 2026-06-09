package com.romi.mogumogu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetSelectionHistoryQuery {

    @Schema(description = "頁碼")
    @Default
    @Min(value = 1)
    private Integer page = 1;

    @Schema(description = "每頁筆數")
    @Default
    @Min(value = 1)
    private Integer limit = 10;
}
