package com.romi.mogumogu.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.romi.mogumogu.constant.DateTimePatternConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    @Schema(description = "結果")
    private String result;

    @Schema(description = "狀態碼")
    private int statusCode;

    @Schema(description = "訊息")
    private String message;

    @Schema(description = "錯誤代碼")
    private String code;

    @Schema(description = "路徑")
    private String path;

    @Schema(description = "時間", pattern = DateTimePatternConstants.STANDARD_DATE_TIME)
    @JsonFormat(pattern = DateTimePatternConstants.STANDARD_DATE_TIME)
    private LocalDateTime timestamp;
}
