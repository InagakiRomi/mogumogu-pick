package com.romi.mogumogu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "MoguMogu Pick API"))
public class MogumoguApplication {

	public static void main(String[] args) {
		SpringApplication.run(MogumoguApplication.class, args);
		System.out.println("Swagger UI: http://localhost:8080/swagger-ui.html");
	}

}
