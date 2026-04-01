package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.request.FindAllUsersRequest;
import com.example.bankcards.dto.response.CreateUserResponse;
import com.example.bankcards.dto.response.FindUserResponse;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.exception.ErrorResponseDto;
import com.example.bankcards.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Validated
@Tag(name = "Admin User Management", description = "Administrative endpoints for managing users, including creation, blocking, and viewing all users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves detailed information about a specific user by their unique identifier. Accessible only to users with ADMIN role.")
    @GetMapping("/{userId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FindUserResponse.class))),
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

    @Operation(
            summary = "Create new user",
            description = "Creates a new user account with the specified details. Accessible only to users with ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateUserResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = adminUserService.createUser(request);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(
            summary = "Block user",
            description = "Blocks a user account by setting enabled status to false. Blocked users cannot access the system. Accessible only to users with ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User blocked successfully (enable = false)"),
            @ApiResponse(responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PutMapping("/{userId}/block")
    public ResponseEntity<Void> blockUser(
            @Parameter(description = "User ID", required = true, example = "7548c54e-b75c-44eb-a8ff-ee0a64aebb5e")
            @PathVariable UUID userId) {
        adminUserService.blockUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get all users",
            description = "Retrieves a paginated list of users with optional filters for search term, role, and enabled status. Accessible only to users with ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FindUserResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation exception (invalid page or size parameters)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @GetMapping
    public Page<FindUserResponse> findAllUsers(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be >= 0")
            int page,

            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be >= 1")
            int size,

            @Parameter(description = "Search term for filtering users by email, full name. " +
                    "Supports partial matching (e.g., 'john' will match 'john.doe@example.com')",
                    example = "john")
            @RequestParam(required = false)
            String search,

            @Parameter(description = "Filter by user role",
                    example = "ADMIN",
                    schema = @Schema(implementation = UserRole.class,
                            allowableValues = {"USER", "ADMIN"}))
            @RequestParam(required = false)
            UserRole role,

            @Parameter(description = "Filter by account status",
                    example = "true",
                    schema = @Schema(description = "true for enabled accounts, false for disabled"))
            @RequestParam(required = false)
            Boolean isEnabled
    ) {
        return adminUserService
                .findAll(new FindAllUsersRequest(page, size, search, role, isEnabled));
    }
}
