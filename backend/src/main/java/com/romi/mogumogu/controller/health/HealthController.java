package com.romi.mogumogu.controller.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.romi.mogumogu.Response.HealthResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/health")
@Tag(name = "health", description = "服務健康檢查")
public class HealthController {

    @GetMapping("")
    @Operation(summary = "喚醒服務")
    @SecurityRequirements
    public HealthResponse wake() {
        return HealthResponse.builder()
                .status("ok")
                .build();
    }
}
