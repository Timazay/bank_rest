package com.example.bankcards.controller;

import com.example.bankcards.dto.request.BlockCardRequest;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.DeleteCardRequest;
import com.example.bankcards.dto.request.FindAllCardRequest;
import com.example.bankcards.dto.response.CreateCardResponse;
import com.example.bankcards.dto.response.FindCardResponse;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;
import com.example.bankcards.exception.ErrorResponseDto;
import com.example.bankcards.service.AdminCardService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/admin/cards")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin Card Management", description = "Administrative endpoints for managing cards, including creation, blocking, activation, deletion, and viewing all cards")
public class AdminCardController {

    private final AdminCardService adminCardService;

    @Operation(
            summary = "Create a new card",
            description = "Creates a new card for a user. Accessible only to users with ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "returns cardId",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateCardRequest.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation exception",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping
    public ResponseEntity<CreateCardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCardService.createCard(request));
    }

    @Operation(
            summary = "Block a card",
            description = "Blocks a card by changing its status to BLOCKED. Blocked cards cannot be used for transactions. Accessible only to users with ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Ok"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation exception",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PutMapping("/{cardId}/block")
    public ResponseEntity<Void> blockCard(
            @PathVariable UUID cardId,
            @Valid @RequestBody BlockCardRequest request) {
        adminCardService.changeCardStatusToBlocked(cardId, request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "Activate a card",
            description = "Activates a card by changing its status to ACTIVE. Activated cards can be used for transactions. Accessible only to users with ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Ok"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation exception",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PutMapping("/{cardId}/activate")
    public ResponseEntity<Void> activateCard(@PathVariable UUID cardId) {
        adminCardService.changeCardStatusToActivate(cardId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "Delete a card",
            description = "Soft deletes (if softDelete) a card by changing its status to DELETED. Accessible only to users with ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Ok"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation exception",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable UUID cardId,
            @Valid @RequestBody DeleteCardRequest request) {
        adminCardService.changeCardStatusToDelete(cardId, request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "Get all cards",
            description = "Retrieves a paginated list of all cards with optional filters. Accessible only to users with ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Ok",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FindCardResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation exception",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping
    public Page<FindCardResponse> getAllCards(
            @RequestParam @Min(value = 0, message = "Page must be >= 0") int page,
            @RequestParam @Min(value = 1, message = "Size must be >= 1") int size,
            @RequestParam(required = false) CardType cardType,
            @RequestParam(required = false) Currency currency,
            @RequestParam(required = false) CardStatus status) {
        return adminCardService
                .findAllCards(new FindAllCardRequest(page, size, null, cardType, status, currency));
    }

    @Operation(
            summary = "Get card by ID",
            description = "Retrieves detailed information about a specific card by its unique identifier. Accessible only to users with ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Ok",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FindCardResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping("/{cardId}")
    public FindCardResponse getCardById(@PathVariable UUID cardId) {
        return adminCardService.findCardById(cardId);
    }
}
