package com.romi.mogumogu.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponse;
import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponseContains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SuppressWarnings("null")
public class GlobalExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void testHandleException_withMessage_shouldReturn500AndMessage() throws Exception {
        assertErrorResponse(
                mockMvc().perform(get("/test/exception-with-message")),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "/test/exception-with-message",
                "boom");
    }

    @Test
    void testHandleException_withoutMessage_shouldFallbackToReasonPhrase() throws Exception {
        assertErrorResponse(
                mockMvc().perform(get("/test/exception-without-message")),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "/test/exception-without-message",
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }

    @Test
    void testHandleMethodArgumentNotValidException_shouldReturn400AndEnglishMessages() throws Exception {
        CreateDto body = new CreateDto(
                "",         // NotBlank -> name is required
                "123456",   // Size -> code size is out of allowed range
                "notEmail", // Email -> email must be a valid email address
                0           // Positive -> age must be greater than 0
        );

        assertErrorResponseContains(
                mockMvc().perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))),
                400,
                "BAD_REQUEST",
                "/test/validate",
                "name is required")
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("name is required"),
                        containsString("code size is out of allowed range"),
                        containsString("email must be a valid email address"),
                        containsString("age must be greater than 0")
                )));
    }

    @Test
    void testHandleMethodArgumentNotValidException_emptyFieldErrors_shouldFallbackToValidationFailed() throws Exception {
        assertErrorResponse(
                mockMvc().perform(get("/test/manual-validation-empty-errors")),
                HttpStatus.BAD_REQUEST,
                "/test/manual-validation-empty-errors",
                "Validation failed");
    }

    @Test
    void testHandleMethodArgumentNotValidException_duplicateMessages_shouldBeDeduped() throws Exception {
        assertErrorResponse(
                mockMvc().perform(get("/test/manual-validation-duplicate-messages")),
                HttpStatus.BAD_REQUEST,
                "/test/manual-validation-duplicate-messages",
                "name is required")
                .andExpect(jsonPath("$.message").value(not(containsString(";"))));
    }

    @Test
    void testHandleMethodArgumentNotValidException_moreValidationCodes_shouldMapToEnglish() throws Exception {
        MoreCodesDto body = new MoreCodesDto(
                -1,         // PositiveOrZero -> count must be greater than or equal to 0
                0,          // Min(1) -> minValue must be greater than or equal to the minimum value
                101,        // Max(100) -> maxValue must be less than or equal to the maximum value
                "abc"       // Pattern -> token format is invalid
        );

        assertErrorResponseContains(
                mockMvc().perform(post("/test/validate-more-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))),
                400,
                "BAD_REQUEST",
                "/test/validate-more-codes",
                "count must be greater than or equal to 0")
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("count must be greater than or equal to 0"),
                        containsString("minValue must be greater than or equal to the minimum value"),
                        containsString("maxValue must be less than or equal to the maximum value"),
                        containsString("token format is invalid")
                )));
    }

    @Test
    void testHandleMethodArgumentNotValidException_unknownOrNullCode_shouldFallbackToInvalid() throws Exception {
        assertErrorResponseContains(
                mockMvc().perform(get("/test/manual-validation-unknown-and-null-code")),
                400,
                "BAD_REQUEST",
                "/test/manual-validation-unknown-and-null-code",
                "customField is invalid")
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("customField is invalid"),
                        containsString("nullCodeField is invalid")
                )));
    }

    @Test
    void testHandleResponseStatusException_withReason_shouldUseReason() throws Exception {
        assertErrorResponse(
                mockMvc().perform(get("/test/status-exception-with-reason")),
                HttpStatus.NOT_FOUND,
                "/test/status-exception-with-reason",
                "restaurant not found");
    }

    @Test
    void testHandleResponseStatusException_withoutReason_shouldFallbackToReasonPhrase() throws Exception {
        assertErrorResponse(
                mockMvc().perform(get("/test/status-exception-without-reason")),
                HttpStatus.FORBIDDEN,
                "/test/status-exception-without-reason",
                HttpStatus.FORBIDDEN.getReasonPhrase());
    }

    @Test
    void testHandleResponseStatusException_blankReason_shouldFallbackToReasonPhrase() throws Exception {
        assertErrorResponse(
                mockMvc().perform(get("/test/status-exception-with-blank-reason")),
                HttpStatus.CONFLICT,
                "/test/status-exception-with-blank-reason",
                HttpStatus.CONFLICT.getReasonPhrase());
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {
        @GetMapping("/exception-with-message")
        public String exceptionWithMessage() {
            throw new RuntimeException("boom");
        }

        @GetMapping("/exception-without-message")
        public String exceptionWithoutMessage() {
            throw new RuntimeException();
        }

        @GetMapping("/status-exception-with-reason")
        public String statusExceptionWithReason() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "restaurant not found");
        }

        @GetMapping("/status-exception-without-reason")
        public String statusExceptionWithoutReason() {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        @GetMapping("/status-exception-with-blank-reason")
        public String statusExceptionWithBlankReason() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "   ");
        }

        @PostMapping("/validate")
        public String validate(@Valid @RequestBody CreateDto dto) {
            return "ok";
        }

        @PostMapping("/validate-more-codes")
        public String validateMoreCodes(@Valid @RequestBody MoreCodesDto dto) {
            return "ok";
        }

        @GetMapping("/manual-validation-empty-errors")
        public String manualValidationEmptyErrors() throws Exception {
            throw new MethodArgumentNotValidException(methodParameter("validate"), new BeanPropertyBindingResult(new Object(), "dto"));
        }

        @GetMapping("/manual-validation-duplicate-messages")
        public String manualValidationDuplicateMessages() throws Exception {
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
            // 同一個 field + code 會映射成同一句英文訊息，handler 會 distinct 去重
            bindingResult.addError(new FieldError("dto", "name", null, false, new String[]{"NotBlank"}, null, "ignored"));
            bindingResult.addError(new FieldError("dto", "name", null, false, new String[]{"NotBlank"}, null, "ignored again"));
            throw new MethodArgumentNotValidException(methodParameter("validate"), bindingResult);
        }

        @GetMapping("/manual-validation-unknown-and-null-code")
        public String manualValidationUnknownAndNullCode() throws Exception {
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
            // unknown code -> default 分支 -> "<field> is invalid"
            bindingResult.addError(new FieldError("dto", "customField", null, false, new String[]{"TotallyUnknown"}, null, "ignored"));
            // null code -> code 為空 -> "<field> is invalid"
            bindingResult.addError(new FieldError("dto", "nullCodeField", null, false, null, null, "ignored"));
            throw new MethodArgumentNotValidException(methodParameter("validate"), bindingResult);
        }

        private static MethodParameter methodParameter(String methodName) throws NoSuchMethodException {
            return new MethodParameter(TestController.class.getDeclaredMethod(methodName, CreateDto.class), 0);
        }
    }

    record CreateDto(
            @NotBlank String name,
            @Size(min = 2, max = 5) String code,
            @Email String email,
            @Positive Integer age
    ) {
    }

    record MoreCodesDto(
            @PositiveOrZero Integer count,
            @Min(1) Integer minValue,
            @Max(100) Integer maxValue,
            @Pattern(regexp = "^[A-Z]{3}\\d{3}$") String token
    ) {
    }
}
