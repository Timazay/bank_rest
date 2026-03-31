package com.example.bankcards.dto.request;

import java.util.UUID;

public record BalanceRequest(
        String email,
        UUID cardId
) {
}
