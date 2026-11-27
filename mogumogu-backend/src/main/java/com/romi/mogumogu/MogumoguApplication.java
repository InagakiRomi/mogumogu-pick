package com.romi.mogumogu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.romi.mogumogu.config.AppConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class MogumoguApplication {
	static AppConfig appConfig;

	@Autowired
	public void setAppConfig(AppConfig config) {
		MogumoguApplication.appConfig = config;
	}

	public static void main(String[] args) {
		SpringApplication.run(MogumoguApplication.class, args);

		log.info("database: " + appConfig.getActiveProfile());
		log.info("Swagger UI: http://localhost:" + appConfig.getServerPort() + "/swagger-ui.html");
	}
}
