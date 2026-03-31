package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCardRequest(
        @NotNull(message = "User ID is required")
        @Schema(example = "123e4567-e89b-12d3-a456-426614174000")
        UUID userId,
        @NotNull(message = "CardType is required")
        @Schema(example = "VISA")
        CardType cardType,
        @NotNull(message = "Currency is required")
        @Schema(example = "USD", defaultValue = "USD")
        Currency currency,
        @Schema(example = "12")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        @NotNull(message = "expiryMonth is required")
        Integer expiryMonth,
        @Schema(example = "2026")
        @NotNull(message = "expiryYear is required")
        Integer expiryYear
) {
}
