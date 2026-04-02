package com.example.bankcards.controller;

import com.example.bankcards.dto.request.BalanceRequest;
import com.example.bankcards.dto.request.CreateApplicationRequest;
import com.example.bankcards.dto.request.FindAllCardRequest;
import com.example.bankcards.dto.request.TopUpBalanceRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CreateApplicationResponse;
import com.example.bankcards.dto.response.FindCardResponse;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;
import com.example.bankcards.exception.ErrorResponseDto;
import com.example.bankcards.service.UserCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
@RequestMapping("/api/v1/user/cards")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('USER')")
@Tag(name = "User Card Management", description = "Endpoints for managing user cards, including viewing cards, checking balances, topping up, and transferring funds")
public class UserCardController {

    private final UserCardService userCardService;

    @Operation(
            summary = "Get user cards",
            description = "Retrieves a paginated list of cards belonging to the authenticated user with optional filters",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cards found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FindCardResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation exception",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping
    public Page<FindCardResponse> getUserCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @Min(value = 0, message = "Page must be >= 0") int page,
            @RequestParam @Min(value = 1, message = "Size must be >= 1") int size,
            @RequestParam(required = false) CardType cardType,
            @RequestParam(required = false) Currency currency,
            @RequestParam(required = false) CardStatus status) {
        return userCardService.findUserCards(
                new FindAllCardRequest(page, size, userDetails.getUsername(), cardType, status, currency)
        );
    }

    @Operation(
            summary = "Get card balance",
            description = "Retrieves the current balance for a specific card belonging to the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Balance retrieved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found or does not belong to user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Card is blocked or deleted",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping("/{cardId}/balance")
    public BalanceResponse getCardBalance(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID cardId) {
        return userCardService.findCardBalance(new BalanceRequest(userDetails.getUsername(), cardId));
    }

    @Operation(
            summary = "Top up card balance",
            description = "Adds funds to a specific card belonging to the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Success",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found or does not belong to user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Card is blocked or deleted",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PutMapping("/{cardId}/balance")
    public ResponseEntity<Void> topUpCardBalance(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID cardId,
            @Valid @RequestBody TopUpBalanceRequest request
    ) {
        userCardService.topUpBalance(userDetails.getUsername(), cardId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Transfer between cards",
            description = "Transfers funds from one card to another. Both cards must belong to the authenticated user and have the same currency",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Transfer completed successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation exception or insufficient funds",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Card is blocked, deleted, or different currency",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping("/transfer")
    public TransferResponse transferBetweenCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request) {
        return userCardService.transferBetweenCards(
                userDetails.getUsername(),
                request
        );
    }

    @Operation(
            summary = "Create card application to Delete, Activate etc...",
            description = "Creates a new application form for a card.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Application form created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateApplicationResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation exception",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Application form already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping("/{cardId}/applications")
    public ResponseEntity<CreateApplicationResponse> createApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID cardId,
            @Valid @RequestBody CreateApplicationRequest request) {
        CreateApplicationResponse response = userCardService.createApplication(
                userDetails.getUsername(),
                cardId,
                request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
