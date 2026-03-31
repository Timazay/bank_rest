package com.example.bankcards.dto.response;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUserResponse(
        @NotNull(message = "userId is required")
        UUID userId
) {
}
