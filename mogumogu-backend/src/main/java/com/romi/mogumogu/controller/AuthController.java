package com.romi.mogumogu.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.romi.mogumogu.dto.auth.LoginRequest;
import com.romi.mogumogu.dto.auth.RegisterRequest;
import com.romi.mogumogu.service.AuthService;
import com.romi.mogumogu.dto.auth.AuthResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "auth", description = "註冊與登入")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "註冊")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }

    @PostMapping("/login")
    @Operation(summary = "登入")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
            @ExampleObject(name = "一般會員", value = """
                    {"email":"amy@test.com", "password":"123"}"""),
            @ExampleObject(name = "群組管理員", value = """
                    {"email":"groupadmin@test.com", "password":"123" }"""),
            @ExampleObject(name = "系統管理員", value = """
                    {"email":"systemadmin@test.com", "password":"123" }""")
    }))
    public AuthResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }
}
