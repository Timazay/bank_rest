package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.response.CreateUserResponse;
import com.example.bankcards.dto.response.FindUserResponse;
import com.example.bankcards.exception.ErrorResponseDto;
import com.example.bankcards.service.AdminUserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/{userId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found successfully"),
            @ApiResponse(responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public FindUserResponse getUserById(
            @Parameter(description = "User ID", required = true, example = "123")
            @PathVariable UUID userId) {
        return adminUserService.findUserById(userId);
    }

    @PostMapping
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = CreateUserResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public CreateUserResponse createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return adminUserService.createUser(request);
    }

    @PutMapping("/{userId}/block")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<Void> blockUser(
            @Parameter(description = "User ID", required = true, example = "123")
            @PathVariable UUID userId) {
        adminUserService.blockUser(userId);
        return ResponseEntity.noContent().build();
    }
}
