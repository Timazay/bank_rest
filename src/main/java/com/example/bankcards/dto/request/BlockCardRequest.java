package com.example.bankcards.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BlockCardRequest(
        @Schema(
                description = "Reason of blocking",
                example = "Card lost or stolen"
        )
        @NotEmpty(message = "blockReason is required")
                @Size(max = 255)
        String blockReason) {
}
