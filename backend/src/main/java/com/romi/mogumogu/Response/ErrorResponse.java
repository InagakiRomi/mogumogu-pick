package com.romi.mogumogu.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.romi.mogumogu.constant.DateTimePatternConstants;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    /** 結果 */
    private String result;

    /** 狀態碼 */
    private int statusCode;

    /** 訊息 */
    private String message;

    /** 錯誤代碼 */
    private String code;

    /** 路徑 */
    private String path;

    /** 時間 */
    @JsonFormat(pattern = DateTimePatternConstants.STANDARD_DATE_TIME)
    private LocalDateTime timestamp;
}
