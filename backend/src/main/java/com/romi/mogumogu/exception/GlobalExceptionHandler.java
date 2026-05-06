package com.romi.mogumogu.exception;

import com.romi.mogumogu.Response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 處理帶有 HTTP 狀態碼的例外 */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {
        // 如果例外訊息為空，則使用 HTTP 狀態碼的預設訊息
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason();
        if (message == null || message.isBlank()) {
            message = status.getReasonPhrase();
        }

        return buildErrorResponse(
                status,
                message,
                request.getRequestURI());
    }

    /** 處理所有例外 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request) {
        // 如果例外訊息為空，則使用預設的內部伺服器錯誤訊息
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
        }

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                message,
                request.getRequestURI());
    }

    /** 建立統一的錯誤回應格式 */
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, String path) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .result("error")
                .statusCode(status.value())
                .message(message)
                .code(status.name())
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }
}
