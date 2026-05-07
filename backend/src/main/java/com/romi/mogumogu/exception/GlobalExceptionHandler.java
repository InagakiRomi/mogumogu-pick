package com.romi.mogumogu.exception;

import com.romi.mogumogu.Response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 處理 DTO 驗證失敗，回傳精簡重點訊息 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors() // 取得所有驗證錯誤
                .stream() // 轉換為串流
                .map(this::toEnglishValidationMessage) // 將驗證錯誤轉換為英文訊息
                .filter(errorMessage -> !errorMessage.isBlank()) // 過濾掉空字串
                .distinct() // 去除重複
                .collect(Collectors.joining("; ")); // 用分號分隔

        // 如果訊息為空，則使用預設的驗證失敗訊息
        if (message.isBlank()) {
            message = "Validation failed";
        }

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI());
    }

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

    /** 將驗證錯誤統一轉為英文訊息，避免受 i18n 設定影響 */
    private String toEnglishValidationMessage(FieldError error) {
        String field = error.getField();
        String code = error.getCode();

        // 如果 code 為空，則回傳錯誤訊息
        if (code == null || code.isBlank()) {
            return field + " is invalid";
        }

        return switch (code) {
            case "NotNull", "NotBlank", "NotEmpty" -> field + " is required";
            case "Size" -> field + " size is out of allowed range";
            case "Min" -> field + " must be greater than or equal to the minimum value";
            case "Max" -> field + " must be less than or equal to the maximum value";
            case "Positive" -> field + " must be greater than 0";
            case "PositiveOrZero" -> field + " must be greater than or equal to 0";
            case "Email" -> field + " must be a valid email address";
            case "Pattern" -> field + " format is invalid";
            default -> field + " is invalid";
        };
    }

}
