package com.romi.mogumogu.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * OpenAPI 文件設定類別。
 * 用於設定 Swagger 文件資訊、JWT 驗證方式，以及 API 標籤的顯示順序。
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_JWT = "bearer-jwt";

    private static final List<String> TAG_ORDER = List.of(
            "auth",
            "groups",
            "restaurants",
            "restaurant-categories",
            "dishes",
            "health");

    /** 建立預設 OpenAPI 文件 */
    @Bean
    OpenAPI mogumoguOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info().title("MoguMogu Pick API"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_JWT, bearerScheme))
                .addSecurityItem(
                        new SecurityRequirement().addList(BEARER_JWT));
    }

    /** 依照預先定義的順序重新排列 Swagger 文件中的 API 標籤 */
    @Bean
    OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> {
            List<Tag> tags = openApi.getTags();

            if (tags == null || tags.isEmpty()) {
                return;
            }

            Map<String, Tag> tagByName = new LinkedHashMap<>();

            for (Tag tag : tags) {
                if (tag == null) {
                    continue;
                }

                String name = tag.getName();
                if (name == null) {
                    continue;
                }

                tagByName.putIfAbsent(name, tag);
            }

            List<Tag> ordered = new ArrayList<>();

            for (String name : TAG_ORDER) {
                Tag tag = tagByName.remove(name);

                if (tag != null) {
                    ordered.add(tag);
                }
            }

            ordered.addAll(tagByName.values());

            openApi.setTags(ordered);
        };
    }
}