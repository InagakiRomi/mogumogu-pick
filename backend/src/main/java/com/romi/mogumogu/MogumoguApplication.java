package com.romi.mogumogu;

import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.romi.mogumogu.logging.JulLoggerFactory;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "MoguMogu Pick API"))
public class MogumoguApplication {

	private static final Logger log = new JulLoggerFactory().printMainLog();

	public static void main(String[] args) {
		SpringApplication.run(MogumoguApplication.class, args);
		log.info("Swagger UI: http://localhost:8080/swagger-ui.html");
	}

}
