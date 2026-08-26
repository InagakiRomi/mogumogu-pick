package com.romi.mogumogu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient overpassRestClient() {
        return RestClient.builder()
                .baseUrl("https://overpass-api.de")
                .defaultHeader("User-Agent", "mogumogu/1.0")
                .build();
    }
}