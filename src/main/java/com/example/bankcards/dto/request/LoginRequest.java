package com.example.bankcards.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Email", example = "user3@example.com")
        @NotBlank(message = "Email is required")
        @Email
        String email,
        @Schema(description = "User password", example = "password123")
        @NotBlank(message = "Password is required")
        String password
) {
}
