package com.example.bankcards.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateUserRequest(
        @Schema(
                description = "User's email address. Must be unique and valid format.",
                example = "john.doe@example.com"
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,
        @Schema(
                description = "User's full name. Can include first and last name.",
                example = "John Doe"
        )
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        @Pattern(regexp = "^[a-zA-Zа-яА-Я\\s'-]+$",
                message = "Full name can only contain letters, spaces, apostrophes, and hyphens")
        String fullName,
        @Schema(
                description = "User's password. Must meet security requirements: at least 8 characters, " +
                        "contain at least one digit, one lowercase, one uppercase, and one special character.",
                example = "SecurePass123!"
        )
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
                message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character (@#$%^&+=!)"
        )
        String password,
        @Schema(
                description = "Flag indicating whether the user should be created with administrator privileges. " +
                        "If set to true, the user will have ADMIN role in addition to the default USER role. " +
                        "Default value is false."
        )
        boolean isAdmin
) {
}
