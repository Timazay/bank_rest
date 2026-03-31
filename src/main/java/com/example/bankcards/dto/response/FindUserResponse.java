package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.UserRole;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record FindUserResponse(
        String fullName,
        String email,
        LocalDateTime createdAt,
        boolean enabled,
        List<UserRole> roleNames
) {
}
