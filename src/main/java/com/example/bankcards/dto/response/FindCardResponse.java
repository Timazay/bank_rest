package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record FindCardResponse(
        UUID cardId,
        String maskedNumber,
        CardType cardType,
        BigDecimal balance,
        Currency currency,
        CardStatus status,
        String blockReason,
        LocalDateTime blockedAt,
        LocalDateTime createdAt
) {
}
