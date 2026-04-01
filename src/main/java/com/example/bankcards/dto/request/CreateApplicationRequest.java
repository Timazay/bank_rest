package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.ApplicationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateApplicationRequest(
        @Schema(description = "Application title",
                example = "Card Deletion Request")
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
        String title,

        @Schema(description = "Detailed description of the application",
                example = "I would like to request deletion of my credit card ending with 1234")
        @NotBlank(message = "Description is required")
        @Size(min = 5, max = 1000, message = "Description must be between 5 and 1000 characters")
        String description,

        @Schema(description = "Type of application",
                example = "DELETE_CARD",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"DELETE_CARD, ACTIVATE_CARD, BLOCK_CARD, ANOTHER_REQUEST"})
        @NotNull(message = "Type is required")
        ApplicationType type
) {
}
