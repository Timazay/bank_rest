package com.example.bankcards.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "Source card ID is required")
        UUID sourceCardId,
        @NotNull(message = "Target card ID is required")
        UUID targetCardId,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "1000000", message = "Amount must not exceed 1,000,000")
        BigDecimal amount,
        String description
) {
}
