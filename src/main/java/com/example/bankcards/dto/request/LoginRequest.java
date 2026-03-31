package com.example.bankcards.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(description = "Email", example = "user3@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,
        @Schema(description = "User password", example = "password123")
        @NotBlank(message = "Password is required")
        @Size(max = 100, message = "Password must not exceed 100 characters")
        String password
) {
}
