package com.romi.mogumogu;

import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

import com.romi.mogumogu.config.JwtTokenProvider;
import com.romi.mogumogu.logging.JulLoggerFactory;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@EnableConfigurationProperties(JwtTokenProvider.class)
@OpenAPIDefinition(info = @Info(title = "MoguMogu Pick API"))
public class MogumoguApplication {

	private static final Logger log = new JulLoggerFactory().printMainLog();

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(MogumoguApplication.class, args);

		// 取得 port 號碼
		String port = context.getEnvironment().getProperty("server.port", "8080");

		// 印出 Swagger UI 網址
		log.info("Swagger UI: http://localhost:" + port + "/swagger-ui.html");
	}

}
