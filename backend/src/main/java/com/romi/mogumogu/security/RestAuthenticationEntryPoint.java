package com.romi.mogumogu.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.romi.mogumogu.Response.ErrorResponse;
import com.romi.mogumogu.exception.ErrorResponseFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 未帶有效 JWT 或尚未登入時觸發 */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        writeErrorJson(response, HttpStatus.UNAUTHORIZED, authException.getMessage(), request.getRequestURI());
    }

    /** 已認證但角色或權限不符合路徑規則時觸發 */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        writeErrorJson(
                response,
                HttpStatus.FORBIDDEN,
                accessDeniedException.getMessage(),
                request.getRequestURI());
    }

    /** 寫入錯誤回應的 JSON 到 response */
    private void writeErrorJson(HttpServletResponse response, HttpStatus status, String message, String path)
            throws IOException {
        ErrorResponse body = ErrorResponseFactory.create(status, message, path);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
