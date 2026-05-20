package com.romi.mogumogu.exception;

import com.romi.mogumogu.Response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public final class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    /**
     * 建立標準格式的錯誤回應
     *
     * @param status  HTTP 狀態
     * @param message 錯誤訊息
     * @param path    發生錯誤的請求路徑
     */
    public static ErrorResponse create(HttpStatus status, String message, String path) {
        String resolvedMessage = message;

        // 如果錯誤訊息為空，則使用 HTTP 狀態的預設訊息
        if (resolvedMessage == null || resolvedMessage.isBlank()) {
            resolvedMessage = status.getReasonPhrase();
        }

        return new ErrorResponse(
                "error",
                status.value(),
                resolvedMessage,
                status.name(),
                path,
                LocalDateTime.now());
    }

    /**
     * 建立帶對應 HTTP 狀態碼
     * 
     * @param status  HTTP 狀態
     * @param message 錯誤訊息
     * @param path    發生錯誤的請求路徑
     */
    public static ResponseEntity<ErrorResponse> toResponseEntity(
            HttpStatus status, String message, String path) {
        return ResponseEntity.status(status.value()).body(create(status, message, path));
    }
}
