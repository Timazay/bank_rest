package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.UserRole;
import lombok.Builder;

@Builder
public record FindAllUsersRequest(
        int page,
        int size,
        String search,
        UserRole userRole,
        Boolean isEnabled
) {
}
