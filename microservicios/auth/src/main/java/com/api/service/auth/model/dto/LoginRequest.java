package com.api.service.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record LoginRequest(
        @NotEmpty @Email String email,
        @NotEmpty String password) {
}