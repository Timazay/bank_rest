package com.example.bankcards.util;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenException;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.List;

@UtilityClass
public class CardValidationUtils {

    public static void validateStatus(List<CardStatus> status) {
        status.stream()
                .filter(e -> e != CardStatus.ACTIVE)
                .forEach(e -> {
                    throw new ForbiddenException("Card status is not active");
                });
    }

    public static void validateCardsForTransfer(Card fromCard, Card toCard, BigDecimal totalAmount) {
        validateStatus(List.of(fromCard.getStatus(), toCard.getStatus()));

        if (fromCard.getCurrency() != toCard.getCurrency())
            throw new BadRequestException(
                    String.format("Cannot transfer between different currencies: %s -> %s",
                            fromCard.getCurrency(), toCard.getCurrency()));


        if (fromCard.getBalance().compareTo(totalAmount) < 0)
            throw new BadRequestException(
                    String.format("Insufficient balance for transfer. Available: %s, Required: %s",
                            totalAmount,
                            fromCard.getBalance()));
    }
}
