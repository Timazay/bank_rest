package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.Currency;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BalanceResponse(
        String maskedNumber,
        BigDecimal balance,
        CardStatus status,
        Currency currency
) {
}
