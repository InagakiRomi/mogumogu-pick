package com.romi.mogumogu.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.romi.mogumogu.exception.ErrorResponseFactory;

@DisplayName("RestAuthenticationEntryPoint")
class RestAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private RestAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        entryPoint = new RestAuthenticationEntryPoint(objectMapper);
    }

    @Test
    @DisplayName("commence 回傳 401 JSON")
    void commence_writes401Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/restaurants");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Bad credentials"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.path("result").asText()).isEqualTo("error");
        assertThat(body.path("statusCode").asInt()).isEqualTo(401);
        assertThat(body.path("code").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(body.path("path").asText()).isEqualTo("/restaurants");
        assertThat(body.path("message").asText()).isEqualTo("Bad credentials");
    }

    @Test
    @DisplayName("handle 回傳 403 JSON")
    void handle_writes403Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/restaurants");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.handle(request, response, new AccessDeniedException("Access is denied"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.path("statusCode").asInt()).isEqualTo(403);
        assertThat(body.path("code").asText()).isEqualTo("FORBIDDEN");
        assertThat(body.path("message").asText()).isEqualTo("Access is denied");
    }

    @Test
    @DisplayName("輸出格式與 ErrorResponseFactory 一致")
    void output_matchesErrorResponseFactoryShape() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("test"));

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        var expected = ErrorResponseFactory.create(HttpStatus.UNAUTHORIZED, "test", "/auth/login");
        assertThat(body.path("result").asText()).isEqualTo(expected.getResult());
        assertThat(body.path("code").asText()).isEqualTo(expected.getCode());
    }
}
