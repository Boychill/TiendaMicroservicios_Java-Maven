package com.api.service.auth.controller;

import com.api.service.auth.model.dto.AuthResponse;
import com.api.service.auth.model.dto.LoginRequest;
import com.api.service.auth.model.dto.RegisterRequest;
import com.api.service.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para registrar y loguear usuarios")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar un nuevo usuario")
    @ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente")
    @ApiResponse(responseCode = "400", description = "Email ya está en uso")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        // Manejo de excepción simple (se puede mejorar con @ControllerAdvice)
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    @ApiResponse(responseCode = "200", description = "Login exitoso")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        // El AuthenticationManager arrojará una excepción si falla (ej.
        // BadCredentialsException)
        // que será manejada por Spring Security y devolverá 401/403.
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/test-auth")
    @Operation(summary = "Endpoint de prueba protegido")
    @ApiResponse(responseCode = "200", description = "Token validado")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    public ResponseEntity<String> testAuthentication() {
        return ResponseEntity.ok("¡Token validado! Estás autenticado. (Java Version)");
    }
}