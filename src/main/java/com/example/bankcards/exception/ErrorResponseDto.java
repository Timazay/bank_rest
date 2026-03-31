package com.example.bankcards.exception;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ErrorResponseDto(
        String uri,
        String status,
        String message,
        String timestamp
) {
    public ErrorResponseDto(String status, String message) {
        this(
                ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString(),
                status,
                message,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
