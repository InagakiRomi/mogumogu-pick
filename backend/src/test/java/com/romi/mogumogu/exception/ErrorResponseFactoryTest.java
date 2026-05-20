package com.romi.mogumogu.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static java.util.Objects.requireNonNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.romi.mogumogu.Response.ErrorResponse;

@DisplayName("ErrorResponseFactory")
class ErrorResponseFactoryTest {

    @Test
    @DisplayName("create 建立標準錯誤格式")
    void create_buildsStandardErrorResponse() {
        ErrorResponse response = ErrorResponseFactory.create(
                HttpStatus.BAD_REQUEST, "field is required", "/test/path");

        assertThat(response.getResult()).isEqualTo("error");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getMessage()).isEqualTo("field is required");
        assertThat(response.getCode()).isEqualTo("BAD_REQUEST");
        assertThat(response.getPath()).isEqualTo("/test/path");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("空白 message 回退至 HTTP reason phrase")
    void create_blankMessage_fallsBackToReasonPhrase() {
        ErrorResponse response = ErrorResponseFactory.create(HttpStatus.NOT_FOUND, "   ", "/x");

        assertThat(response.getMessage()).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
    }

    @Test
    @DisplayName("null message 回退至 HTTP reason phrase")
    void create_nullMessage_fallsBackToReasonPhrase() {
        ErrorResponse response = ErrorResponseFactory.create(HttpStatus.FORBIDDEN, null, "/x");

        assertThat(response.getMessage()).isEqualTo(HttpStatus.FORBIDDEN.getReasonPhrase());
    }

    @Test
    @DisplayName("toResponseEntity 帶正確 HTTP 狀態")
    void toResponseEntity_wrapsWithStatus() {
        ResponseEntity<ErrorResponse> entity =
                ErrorResponseFactory.toResponseEntity(HttpStatus.CONFLICT, "duplicate", "/auth/register");

        assertThat(entity.getStatusCode().value()).isEqualTo(409);
        ErrorResponse responseBody = requireNonNull(entity.getBody());
        assertThat(responseBody.getMessage()).isEqualTo("duplicate");
        assertThat(responseBody.getCode()).isEqualTo("CONFLICT");
    }
}
