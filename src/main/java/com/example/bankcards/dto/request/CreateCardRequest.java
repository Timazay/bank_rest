package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCardRequest(
        @NotNull(message = "User ID is required")
        @Schema(example = "123e4567-e89b-12d3-a456-426614174000")
        UUID userId,
        @Schema(example = "VISA")
        CardType cardType,

        @Schema(example = "USD", defaultValue = "USD")
        Currency currency
) {
}
