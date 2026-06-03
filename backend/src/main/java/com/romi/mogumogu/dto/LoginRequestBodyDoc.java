package com.romi.mogumogu.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.http.MediaType;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = LoginRequest.class), examples = {
                @ExampleObject(name = "Super", value = "{\"email\":\"super@test.com\",\"password\":\"123\"}"),
                @ExampleObject(name = "GroupAdmin", value = "{\"email\":\"groupadmin@test.com\",\"password\":\"123\"}"),
                @ExampleObject(name = "User", value = "{\"email\":\"user@test.com\",\"password\":\"123\"}")
}))
public @interface LoginRequestBodyDoc {
}
