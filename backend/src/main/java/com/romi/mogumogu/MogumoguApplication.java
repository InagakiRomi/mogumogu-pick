package com.romi.mogumogu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "MoguMogu Pick API"))
public class MogumoguApplication {

	private static final Logger log = LoggerFactory.getLogger(MogumoguApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(MogumoguApplication.class, args);
		log.info("Swagger UI: http://localhost:8080/swagger-ui.html");
	}

}
