package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.UserRole;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record FindUserResponse(
        UUID userId,
        String fullName,
        String email,
        LocalDateTime createdAt,
        boolean enabled,
        List<UserRole> roleNames
) {
}
