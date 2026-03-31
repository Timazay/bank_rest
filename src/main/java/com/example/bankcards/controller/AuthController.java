package com.example.bankcards.controller;


import com.example.bankcards.dto.response.AccessTokenResponse;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshTokenRequest;
import com.example.bankcards.exception.ErrorResponseDto;
import com.example.bankcards.service.AuthService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns refresh and access token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginRequest.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation exception",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Auth exception",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.authenticate(request);
    }

    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tasks found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RefreshTokenRequest.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation exception",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Auth exception",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PutMapping("/refresh")
    public AccessTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }
}
