package com.romi.mogumogu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class AppConfig {
    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${spring.profiles.active:h2}")
    private String activeProfile;
}
