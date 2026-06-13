package com.romi.mogumogu.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

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
                .components(new Components().addSecuritySchemes(BEARER_JWT, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }

    /** SpringDoc 掃描 Controller 後會重排 tags，在此依指定順序還原 */
    @Bean
    OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> {
            if (openApi.getTags() == null || openApi.getTags().isEmpty()) {
                return;
            }
            Map<String, Tag> tagByName = openApi.getTags().stream()
                    .collect(Collectors.toMap(Tag::getName, tag -> tag, (left, right) -> left, LinkedHashMap::new));
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
