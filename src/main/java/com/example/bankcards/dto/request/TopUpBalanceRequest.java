package com.example.bankcards.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record TopUpBalanceRequest(
        @NotNull(message = "Balance required")
        @DecimalMin(value = "0.01", message = "Balance must be positive")
        @Digits(integer = 10, fraction = 2, message = "Balance must have valid format")
        BigDecimal balance
) {
        public TopUpBalanceRequest {
                if (balance != null) {
                        balance = balance.setScale(2, RoundingMode.HALF_UP);
                }
        }
}
