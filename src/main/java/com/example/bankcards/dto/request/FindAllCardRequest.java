package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;
import lombok.Builder;

@Builder
public record FindAllCardRequest(
        int page,
        int size,
        String userEmail,
        CardType cardType,
        CardStatus cardStatus,
        Currency currency
) {
}
