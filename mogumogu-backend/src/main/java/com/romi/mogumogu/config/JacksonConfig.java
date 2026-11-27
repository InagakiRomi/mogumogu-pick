package com.romi.mogumogu.config;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public JsonMapperBuilderCustomizer jsonMapperCustomizer() {
        return builder -> builder.withConfigOverride(LocalDateTime.class,
                config -> config.setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd HH:mm:ss")));
    }
}