package com.romi.mogumogu.testutil;

import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class ErrorResponseTestUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ErrorResponseTestUtils() {}

    public static final String DEFAULT_TIMESTAMP_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    public static ResultActions assertErrorResponse(
            ResultActions resultActions,
            HttpStatus status,
            String path,
            String message) throws Exception {
        return assertErrorResponse(resultActions, status.value(), status.name(), path, message);
    }

    public static ResultActions assertErrorResponse(
            ResultActions resultActions,
            int statusCode,
            String code,
            String path,
            String message) throws Exception {
        return resultActions
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.result").value("error"))
                .andExpect(jsonPath("$.statusCode").value(statusCode))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(Objects.requireNonNull(jsonPath("$.path").value(path)))
                .andExpect(errorTimestampMatchesDefaultPattern());
    }

    public static ResultActions assertErrorResponseContains(
            ResultActions resultActions,
            int statusCode,
            String code,
            String path,
            String messagePart) throws Exception {
        return resultActions
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.result").value("error"))
                .andExpect(jsonPath("$.statusCode").value(statusCode))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(Objects.requireNonNull(jsonPath("$.path").value(path)))
                .andExpect(errorMessageContains(messagePart))
                .andExpect(errorTimestampMatchesDefaultPattern());
    }

    @NonNull
    private static ResultMatcher errorMessageContains(String expectedPart) {
        return new ResultMatcher() {
            @Override
            public void match(@NonNull MvcResult result) throws Exception {
                JsonNode root = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
                String message = root.path("message").asText();
                if (!message.contains(expectedPart)) {
                    throw new AssertionError(
                            "expected $.message containing \"" + expectedPart + "\" but was: " + message);
                }
            }
        };
    }

    @NonNull
    private static ResultMatcher errorTimestampMatchesDefaultPattern() {
        Pattern pattern = Pattern.compile(DEFAULT_TIMESTAMP_REGEX);
        return new ResultMatcher() {
            @Override
            public void match(@NonNull MvcResult result) throws Exception {
                JsonNode root = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
                String timestamp = root.path("timestamp").asText();
                if (!pattern.matcher(timestamp).matches()) {
                    throw new AssertionError(
                            "expected $.timestamp matching "
                                    + DEFAULT_TIMESTAMP_REGEX
                                    + " but was: "
                                    + timestamp);
                }
            }
        };
    }
}
