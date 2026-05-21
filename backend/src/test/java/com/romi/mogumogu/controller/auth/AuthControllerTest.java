package com.romi.mogumogu.controller.auth;

import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.DEFAULT_TIMESTAMP_REGEX;
import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponse;
import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponseContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.romi.mogumogu.Response.LoginResponse;
import com.romi.mogumogu.dto.LoginRequest;
import com.romi.mogumogu.dto.RegisterRequest;
import com.romi.mogumogu.enums.UserRole;
import com.romi.mogumogu.exception.GlobalExceptionHandler;
import com.romi.mogumogu.service.auth.AuthService;
import com.romi.mogumogu.testsupport.TestSecurityConfig;

@WebMvcTest(controllers = AuthController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class AuthControllerTest {

    private static final String AUTH_LOGIN_PATH = "/auth/login";
    private static final String AUTH_REGISTER_PATH = "/auth/register";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    private static final String CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void register_success_returns200AndPayload() throws Exception {
        RegisterRequest body = registerRequest("新使用者", "new@example.com", "password123");
        Date created = new Date(1_700_000_000_000L);
        Date modified = new Date(1_700_000_060_000L);

        when(authService.register(any(RegisterRequest.class))).thenReturn(LoginResponse.builder()
                .userId(10)
                .groupId(1)
                .email("new@example.com")
                .username("新使用者")
                .role(UserRole.USER.ordinal())
                .createdAt(created)
                .updatedAt(modified)
                .build());

        String registerJson = performRegister(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.groupId").value(1))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.username").value("新使用者"))
                .andExpect(jsonPath("$.role").value(UserRole.USER.ordinal()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(
                !objectMapper.readTree(registerJson).hasNonNull("token"),
                "註冊回應不應帶有非 null 的 token");

        verify(authService).register(argThat(req -> "\u65b0\u4f7f\u7528\u8005".equals(req.getUsername()) && "new@example.com".equals(req.getEmail()) && "password123".equals(req.getPassword())));
    }

    @Test
    void register_conflict_returns409AndErrorPayload() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "此電子郵件已被註冊"));

        assertRegisterErrorResponse(
                registerRequest("u", "taken@example.com", "password123"),
                HttpStatus.CONFLICT,
                "此電子郵件已被註冊");
    }

    @Test
    void register_passwordTooShort_returns400AndSkipsService() throws Exception {
        assertBadRequestValidationRegister(
                registerRequest("u", "ok@example.com", "short"),
                "password size is out of allowed range");
    }

    @Test
    void register_blankUsername_returns400AndSkipsService() throws Exception {
        assertBadRequestValidationRegister(
                registerRequest("  \t\n ", "ok@example.com", "password123"),
                "username is required");
    }

    @Test
    void register_blankEmail_returns400AndSkipsService() throws Exception {
        assertBadRequestValidationRegister(
                registerRequest("validuser", "   \t\n", "password123"),
                "email is required");
    }

    @Test
    void register_invalidEmailFormat_returns400AndSkipsService() throws Exception {
        assertBadRequestValidationRegister(
                registerRequest("validuser", "not-an-email", "password123"),
                "email must be a valid email address");
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-at-sign", "@nodomain", "spaces in@mail.com"})
    void register_invalidEmailVariants_returns400(String badEmail) throws Exception {
        assertBadRequestValidationRegister(
                registerRequest("u", badEmail, "password123"),
                "email must be a valid email address");
    }

    @Test
    void register_blankPassword_returns400AndSkipsService() throws Exception {
        assertBadRequestValidationRegister(
                registerRequest("validuser", "ok@example.com", " \t\n "),
                "password is required");
    }

    @Test
    void register_usernameTooLong_returns400AndSkipsService() throws Exception {
        assertBadRequestValidationRegister(
                registerRequest("x".repeat(65), "ok@example.com", "password123"),
                "username size is out of allowed range");
    }

    @Test
    void register_passwordTooLong_returns400AndSkipsService() throws Exception {
        assertBadRequestValidationRegister(
                registerRequest("user", "ok@example.com", "p".repeat(129)),
                "password size is out of allowed range");
    }

    @Test
    void register_nullFieldsViaRawJson_returns400AndSkipsService() throws Exception {
        assertBadRequestValidationRegisterRaw(
                "{\"username\":null,\"email\":null,\"password\":null}",
                "username is required",
                "email is required",
                "password is required");
    }

    @Test
    void register_multipleValidationErrors_messageListsFields() throws Exception {
        assertBadRequestMessageContainsSubstrings(
                performRegister(registerRequest("", "", "")),
                this::verifyRegisterNeverCalled,
                "username",
                "email",
                "password");
    }

    @Test
    void register_missingBody_returns500AndSkipsService() throws Exception {
        assertMissingJsonBodyError(AUTH_REGISTER_PATH);
    }

    @Test
    void register_invalidJson_returns500AndSkipsService() throws Exception {
        assertJsonParseError(AUTH_REGISTER_PATH, performRegisterRaw("{\"username\":\"a\",\"email\":\"a@b.com\",\"password\":\"x\""));
    }

    @Test
    void register_unsupportedContentType_returns500AndSkipsService() throws Exception {
        assertUnsupportedPlainTextPost(
                AUTH_REGISTER_PATH,
                objectMapper.writeValueAsString(registerRequest("u", "u@e.com", "password123")));
    }

    @Test
    void register_badRequest_returns400AndErrorPayload() throws Exception {
        stubRegisterThrows(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invitation code"));
        assertRegisterErrorResponse(defaultRegisterBody(), HttpStatus.BAD_REQUEST, "Invalid invitation code");
    }

    @Test
    void register_unprocessableEntity_returns422AndErrorPayload() throws Exception {
        stubRegisterThrows(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Policy not accepted"));
        assertRegisterErrorResponse(defaultRegisterBody(), HttpStatus.UNPROCESSABLE_ENTITY, "Policy not accepted");
    }

    @Test
    void register_internalServerError_returns500AndErrorPayload() throws Exception {
        stubRegisterThrows(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Mailer unavailable"));
        assertRegisterErrorResponse(defaultRegisterBody(), HttpStatus.INTERNAL_SERVER_ERROR, "Mailer unavailable");
    }

    @Test
    void register_responseStatusWithoutReason_returnsStatusPhrase() throws Exception {
        stubRegisterThrows(new ResponseStatusException(HttpStatus.BAD_REQUEST));
        assertRegisterErrorResponse(defaultRegisterBody(), HttpStatus.BAD_REQUEST, "Bad Request");
    }

    @Test
    void register_serviceThrowsRuntimeException_returns500() throws Exception {
        stubRegisterThrows(new RuntimeException("Unexpected persistence error"));
        assertRegisterErrorResponse(
                defaultRegisterBody(), HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected persistence error");
    }

    @Test
    void register_serviceThrowsRuntimeExceptionWithoutMessage_returns500WithDefaultPhrase() throws Exception {
        stubRegisterThrows(new RuntimeException());
        assertRegisterErrorResponse(defaultRegisterBody(), HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
    }

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
                .role(UserRole.USER.ordinal())
                .createdAt(created)
                .updatedAt(modified)
                .token("stub-access-token")
                .build());

        performLogin(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.groupId").value(42))
                .andExpect(jsonPath("$.email").value("demo@example.com"))
                .andExpect(jsonPath("$.username").value("demo"))
                .andExpect(jsonPath("$.role").value(UserRole.USER.ordinal()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.token").value("stub-access-token"));

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
                .role(role.ordinal())
                .token("stub-token")
                .build());

        performLogin(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role.ordinal()))
                .andExpect(jsonPath("$.token").value("stub-token"));

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
        assertBadRequestMessageContainsSubstrings(
                performLogin(loginRequest("", "")),
                this::verifyLoginNeverCalled,
                "email",
                "password");
    }

    @Test
    void login_missingBody_returns500AndSkipsService() throws Exception {
        assertMissingJsonBodyError(AUTH_LOGIN_PATH);
    }

    @Test
    void login_invalidJson_returns500AndSkipsService() throws Exception {
        assertJsonParseError(AUTH_LOGIN_PATH, performLoginRaw("{\"email\":\"a@b.com\",\"password\":\"x\""));
    }

    @Test
    void login_unsupportedContentType_returns500ErrorPayloadAndSkipsService() throws Exception {
        assertUnsupportedPlainTextPost(AUTH_LOGIN_PATH, objectMapper.writeValueAsString(validLoginBody()));
    }

    @Test
    void login_badRequest_returns400AndErrorPayload() throws Exception {
        stubLoginThrows(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Captcha required"));

        assertLoginErrorResponse(validLoginBody(), HttpStatus.BAD_REQUEST, "Captcha required");
    }

    @Test
    void login_conflict_returns409AndErrorPayload() throws Exception {
        stubLoginThrows(new ResponseStatusException(HttpStatus.CONFLICT, "Already logged in elsewhere"));

        assertLoginErrorResponse(validLoginBody(), HttpStatus.CONFLICT, "Already logged in elsewhere");
    }

    @Test
    void login_tooManyRequests_returns429AndErrorPayload() throws Exception {
        stubLoginThrows(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later"));

        assertLoginErrorResponse(validLoginBody(), HttpStatus.TOO_MANY_REQUESTS, "Try again later");
    }

    @Test
    void login_internalServerError_returns500AndErrorPayload() throws Exception {
        stubLoginThrows(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Auth provider down"));

        assertLoginErrorResponse(validLoginBody(), HttpStatus.INTERNAL_SERVER_ERROR, "Auth provider down");
    }

    @Test
    void login_serviceThrowsRuntimeExceptionWithoutMessage_returns500WithDefaultPhrase() throws Exception {
        stubLoginThrows(new RuntimeException());

        assertLoginErrorResponse(
                validLoginBody(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error");
    }

    @Test
    void login_jsonWithOnlyEmail_returns400AndSkipsService() throws Exception {
        assertBadRequestValidationRaw("{\"email\":\"only@example.com\"}", "password is required");
    }

    @Test
    void login_jsonWithOnlyPassword_returns400AndSkipsService() throws Exception {
        assertBadRequestValidationRaw("{\"password\":\"secret123\"}", "email is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-at-sign", "@nodomain", "bad space@x.com"})
    void login_invalidEmailVariants_returns400(String badEmail) throws Exception {
        assertBadRequestValidation(loginRequest(badEmail, "password123"), "email must be a valid email address");
    }

    @Test
    void login_getMethodNotSupported_returns500ErrorPayloadAndSkipsService() throws Exception {
        assertGetMethodNotSupported(AUTH_LOGIN_PATH);
    }

    @Test
    void register_getMethodNotSupported_returns500ErrorPayloadAndSkipsService() throws Exception {
        assertGetMethodNotSupported(AUTH_REGISTER_PATH);
    }

    private RegisterRequest registerRequest(String username, String email, String password) {
        RegisterRequest body = new RegisterRequest();
        body.setUsername(username);
        body.setEmail(email);
        body.setPassword(password);
        return body;
    }

    /** 多數「服務層丟錯誤」註冊測試共用的合法請求本文 */
    private RegisterRequest defaultRegisterBody() {
        return registerRequest("u", "new@example.com", "password123");
    }

    private void stubRegisterThrows(Throwable throwable) {
        when(authService.register(any(RegisterRequest.class))).thenThrow(throwable);
    }

    private void verifyRegisterNeverCalled() {
        verify(authService, never()).register(any(RegisterRequest.class));
    }

    private void verifyLoginNeverCalled() {
        verify(authService, never()).login(any(LoginRequest.class));
    }

    private void assertBadRequestPayload(ResultActions resultActions, String... messageParts) throws Exception {
        String json = resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("error"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertMessageContainsAll(json, messageParts);
    }

    private void assertBadRequestValidationRegister(RegisterRequest body, String... messageParts) throws Exception {
        assertBadRequestPayload(performRegister(body), messageParts);
        verifyRegisterNeverCalled();
    }

    private void assertBadRequestValidationRegisterRaw(String rawJson, String... messageParts) throws Exception {
        assertBadRequestPayload(performRegisterRaw(rawJson), messageParts);
        verifyRegisterNeverCalled();
    }

    private ResultActions performRegister(RegisterRequest body) throws Exception {
        return mockMvc.perform(post(AUTH_REGISTER_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(body))));
    }

    private ResultActions performRegisterRaw(String json) throws Exception {
        return mockMvc.perform(post(AUTH_REGISTER_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(json, "json")));
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

    private void assertRegisterErrorResponse(RegisterRequest body, HttpStatus status, String message) throws Exception {
        assertErrorResponse(performRegister(body), status, AUTH_REGISTER_PATH, message);
        verify(authService).register(any(RegisterRequest.class));
    }

    private void assertBadRequestValidation(LoginRequest body, String... messageParts) throws Exception {
        assertBadRequestPayload(performLogin(body), messageParts);
        verifyNoInteractions(authService);
    }

    private void assertBadRequestValidationRaw(String rawJson, String... messageParts) throws Exception {
        assertBadRequestPayload(performLoginRaw(rawJson), messageParts);
        verifyNoInteractions(authService);
    }

    /**
     * 驗證 400 回應的 {@code message} 同時包含多個子字串（例如多欄位驗證），並執行自訂的 Mockito 斷言。
     */
    private void assertBadRequestMessageContainsSubstrings(
            ResultActions pendingRequest,
            Runnable verifyServiceNotUsed,
            String... substrings) throws Exception {
        String responseBody = pendingRequest
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        String combined = root.path("message").asText();
        for (String part : substrings) {
            assertTrue(combined.contains(part));
        }
        verifyServiceNotUsed.run();
    }

    private void assertMissingJsonBodyError(String path) throws Exception {
        final String safePath = Objects.requireNonNull(path, "path");
        assertErrorResponseContains(
                mockMvc.perform(post(safePath).contentType(CONTENT_TYPE_JSON)),
                HTTP_INTERNAL_SERVER_ERROR,
                CODE_INTERNAL_SERVER_ERROR,
                safePath,
                "Required request body is missing");
        verifyNoInteractions(authService);
    }

    private void assertJsonParseError(String path, ResultActions truncatedJsonPost) throws Exception {
        final String safePath = Objects.requireNonNull(path, "path");
        assertErrorResponseContains(
                truncatedJsonPost,
                HTTP_INTERNAL_SERVER_ERROR,
                CODE_INTERNAL_SERVER_ERROR,
                safePath,
                "JSON parse error");
        verifyNoInteractions(authService);
    }

    private void assertUnsupportedPlainTextPost(String path, String entityBody) throws Exception {
        final String safePath = Objects.requireNonNull(path, "path");
        final String safeBody = Objects.requireNonNull(entityBody, "entityBody");
        String responseBody = mockMvc.perform(post(safePath)
                        .contentType(CONTENT_TYPE_TEXT_PLAIN)
                        .content(safeBody))
                .andExpect(status().is(HTTP_INTERNAL_SERVER_ERROR))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        assertEquals("error", root.path("result").asText());
        assertEquals(HTTP_INTERNAL_SERVER_ERROR, root.path("statusCode").asInt());
        assertEquals(CODE_INTERNAL_SERVER_ERROR, root.path("code").asText());
        assertEquals(safePath, root.path("path").asText());
        String message = root.path("message").asText();
        assertTrue(
                message.contains("Content-Type 'text/plain;charset=UTF-8' is not supported")
                        || message.contains("Content-Type 'text/plain' is not supported"),
                () -> "unexpected message: " + message);
        String timestamp = root.path("timestamp").asText();
        assertTrue(
                Pattern.compile(DEFAULT_TIMESTAMP_REGEX).matcher(timestamp).matches(),
                () -> "unexpected timestamp: " + timestamp);
        verifyNoInteractions(authService);
    }

    private void assertGetMethodNotSupported(String path) throws Exception {
        final String safePath = Objects.requireNonNull(path, "path");
        assertErrorResponseContains(
                mockMvc.perform(get(safePath)),
                HTTP_INTERNAL_SERVER_ERROR,
                CODE_INTERNAL_SERVER_ERROR,
                safePath,
                "Request method 'GET' is not supported");
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
