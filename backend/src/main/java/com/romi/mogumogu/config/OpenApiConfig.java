package com.romi.mogumogu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_JWT = "bearer-jwt";

    /** 建立預設 OpenAPI 文件 */
    @Bean
    OpenAPI mogumoguOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info().title("MoguMogu Pick API"))
                .components(new Components().addSecuritySchemes(BEARER_JWT, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
