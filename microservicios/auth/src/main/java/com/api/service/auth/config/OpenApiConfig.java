package com.api.service.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "API de Servicio de Autenticación", version = "1.0", description = "Microservicio para gestionar el registro, login y tokens JWT de usuarios."))
@SecurityScheme(name = "bearerAuth", // Nombre usado en @SecurityRequirement
        type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}