package com.api.service.auth.model.dto;

import java.util.UUID;

// Usamos el 'role' como String simple para la respuesta JSON
public record AuthResponse(
        String token,
        UUID userId,
        String email,
        String role) {
}