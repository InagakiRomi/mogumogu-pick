package com.romi.mogumogu.testutil;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
public final class ErrorResponseTestUtils {
    private ErrorResponseTestUtils() {
    }

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
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(DEFAULT_TIMESTAMP_REGEX)));
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
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(messagePart)))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(DEFAULT_TIMESTAMP_REGEX)));
    }
}

