package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;

public record FindAllCardRequest(
        int page,
        int size,
        CardType cardType,
        CardStatus cardStatus,
        Currency currency
) {
}
