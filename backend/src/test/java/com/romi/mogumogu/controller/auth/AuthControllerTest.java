package com.romi.mogumogu.controller.auth;

import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponse;
import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponseContains;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.romi.mogumogu.Response.LoginResponse;
import com.romi.mogumogu.dto.LoginRequest;
import com.romi.mogumogu.enums.UserRole;
import com.romi.mogumogu.exception.GlobalExceptionHandler;
import com.romi.mogumogu.service.auth.AuthService;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    private static final String AUTH_LOGIN_PATH = "/auth/login";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void login_success_returns200AndPayload() throws Exception {
        LoginRequest body = loginRequest("demo@example.com", "password123");
        Date created = new Date(1_700_000_000_000L);
        Date modified = new Date(1_700_000_060_000L);

        stubLoginReturns(LoginResponse.builder()
                .userId(1)
                .groupId(42)
                .email("demo@example.com")
                .username("demo")
                .role(UserRole.USER)
                .createdAt(created)
                .updatedAt(modified)
                .build());

        performLogin(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.groupId").value(42))
                .andExpect(jsonPath("$.email").value("demo@example.com"))
                .andExpect(jsonPath("$.username").value("demo"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(authService).login(argThat(req -> "demo@example.com".equals(req.getEmail())
                && "password123".equals(req.getPassword())));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void login_success_returnsEachRole(UserRole role) throws Exception {
        LoginRequest body = loginRequest("admin@example.com", "secret");

        stubLoginReturns(LoginResponse.builder()
                .userId(99)
                .groupId(1)
                .email("admin@example.com")
                .username("admin")
                .role(role)
                .build());

        performLogin(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role.name()));

        verifyLoginInvokedOnce();
    }

    @Test
    void login_unauthorized_returns401AndErrorPayload() throws Exception {
        stubLoginThrows(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "電子郵件或密碼錯誤"));

        assertLoginErrorResponse(
                loginRequest("demo@example.com", "wrong"),
                HttpStatus.UNAUTHORIZED,
                "電子郵件或密碼錯誤");
    }

    @Test
    void login_unauthorized_withoutReason_returns401WithStatusPhrase() throws Exception {
        stubLoginThrows(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        assertLoginErrorResponse(
                loginRequest("x@y.com", "p"),
                HttpStatus.UNAUTHORIZED,
                "Unauthorized");
    }

    @Test
    void login_forbidden_returns403AndErrorPayload() throws Exception {
        stubLoginThrows(new ResponseStatusException(HttpStatus.FORBIDDEN, "Account disabled"));

        assertLoginErrorResponse(validLoginBody(), HttpStatus.FORBIDDEN, "Account disabled");
    }

    @Test
    void login_notFound_returns404AndErrorPayload() throws Exception {
        stubLoginThrows(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        assertLoginErrorResponse(validLoginBody(), HttpStatus.NOT_FOUND, "Tenant not found");
    }

    @Test
    void login_serviceThrowsRuntimeException_returns500() throws Exception {
        stubLoginThrows(new RuntimeException("Database connection failed"));

        assertLoginErrorResponse(
                validLoginBody(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database connection failed");
    }

    @Test
    void login_blankEmail_returns400AndSkipsService() throws Exception {
        assertBadRequestValidation(loginRequest("   ", "password123"), "email is required");
    }

    @Test
    void login_blankPassword_returns400AndSkipsService() throws Exception {
        assertBadRequestValidation(loginRequest("ok@example.com", " \t\n "), "password is required");
    }

    @Test
    void login_invalidEmailFormat_returns400AndSkipsService() throws Exception {
        assertBadRequestValidation(
                loginRequest("not-an-email", "password123"),
                "email must be a valid email address");
    }

    @Test
    void login_nullEmailAndNullPassword_returns400AndSkipsService() throws Exception {
        assertBadRequestValidationRaw("{}", "email is required", "password is required");
    }

    @Test
    void login_multipleValidationErrors_messageListsBothFields() throws Exception {
        String responseBody = performLogin(loginRequest("", ""))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        String combined = root.path("message").asText();
        assertTrue(combined.contains("email"));
        assertTrue(combined.contains("password"));

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    void login_missingBody_returns500AndSkipsService() throws Exception {
        assertErrorResponseContains(
                mockMvc.perform(post(AUTH_LOGIN_PATH).contentType(CONTENT_TYPE_JSON)),
                500,
                "INTERNAL_SERVER_ERROR",
                AUTH_LOGIN_PATH,
                "Required request body is missing");

        verifyNoInteractions(authService);
    }

    @Test
    void login_invalidJson_returns500AndSkipsService() throws Exception {
        assertErrorResponseContains(
                performLoginRaw("{\"email\":\"a@b.com\",\"password\":\"x\""),
                500,
                "INTERNAL_SERVER_ERROR",
                AUTH_LOGIN_PATH,
                "JSON parse error");

        verifyNoInteractions(authService);
    }

    @Test
    void login_unsupportedContentType_returns500ErrorPayloadAndSkipsService() throws Exception {
        assertErrorResponseContains(
                mockMvc.perform(post(AUTH_LOGIN_PATH)
                        .contentType(CONTENT_TYPE_TEXT_PLAIN)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(validLoginBody())))),
                500,
                "INTERNAL_SERVER_ERROR",
                AUTH_LOGIN_PATH,
                "Content-Type 'text/plain;charset=UTF-8' is not supported");

        verifyNoInteractions(authService);
    }

    private LoginRequest validLoginBody() {
        return loginRequest("user@example.com", "secret");
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest body = new LoginRequest();
        body.setEmail(email);
        body.setPassword(password);
        return body;
    }

    private void stubLoginReturns(LoginResponse response) {
        when(authService.login(any(LoginRequest.class))).thenReturn(response);
    }

    private void stubLoginThrows(Throwable throwable) {
        when(authService.login(any(LoginRequest.class))).thenThrow(throwable);
    }

    private void verifyLoginInvokedOnce() {
        verify(authService).login(any(LoginRequest.class));
    }

    private void assertLoginErrorResponse(LoginRequest body, HttpStatus status, String message) throws Exception {
        assertErrorResponse(performLogin(body), status, AUTH_LOGIN_PATH, message);
        verifyLoginInvokedOnce();
    }

    private void assertBadRequestValidation(LoginRequest body, String... messageParts) throws Exception {
        String json = performLogin(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("error"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertMessageContainsAll(json, messageParts);
        verifyNoInteractions(authService);
    }

    private void assertBadRequestValidationRaw(String rawJson, String... messageParts) throws Exception {
        String json = performLoginRaw(rawJson)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("error"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertMessageContainsAll(json, messageParts);
        verifyNoInteractions(authService);
    }

    private void assertMessageContainsAll(String responseJson, String... messageParts) throws Exception {
        for (String part : messageParts) {
            assertMessageContains(responseJson, part);
        }
    }

    private ResultActions performLogin(LoginRequest body) throws Exception {
        return mockMvc.perform(post(AUTH_LOGIN_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(body))));
    }

    private ResultActions performLoginRaw(String json) throws Exception {
        return mockMvc.perform(post(AUTH_LOGIN_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(json, "json")));
    }

    private void assertMessageContains(String responseJson, String messagePart) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode messageNode = rootNode.get("message");
        assertNotNull(messageNode, "message field should exist");
        assertTrue(
                messageNode.asText().contains(messagePart),
                "message should contain: " + messagePart + " but was: " + messageNode.asText());
    }
}
