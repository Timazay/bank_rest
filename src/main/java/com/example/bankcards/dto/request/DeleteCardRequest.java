package com.example.bankcards.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record DeleteCardRequest(
        @Schema(
                description = "Deletion type: true - soft deletion (status change), false - hard deletion from the database",
                example = "true",
                allowableValues = {"true", "false"}
        )
        @NotNull(message = "softDelete is required")
        Boolean softDelete) {
}
