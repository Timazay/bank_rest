package com.example.bankcards.controller;

import com.example.bankcards.TestSecurityConfig;
import com.example.bankcards.dto.request.BalanceRequest;
import com.example.bankcards.dto.request.CreateApplicationRequest;
import com.example.bankcards.dto.request.FindAllCardRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CreateApplicationResponse;
import com.example.bankcards.dto.response.FindCardResponse;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.enums.ApplicationType;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.service.UserCardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCardController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(authorities = "USER")
public class UserCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserCardService userCardService;

    @Test
    void getUserCards_WhenValidRequest_ThenReturnOkAndPageOfCards() throws Exception {
        int page = 0;
        int size = 10;

        UUID cardId1 = UUID.randomUUID();
        UUID cardId2 = UUID.randomUUID();

        List<FindCardResponse> cards = List.of(
                FindCardResponse.builder()
                        .cardId(cardId1)
                        .maskedNumber("****1234")
                        .balance(new BigDecimal("1000.50"))
                        .currency(Currency.USD)
                        .status(CardStatus.ACTIVE)
                        .cardType(CardType.MIR)
                        .createdAt(LocalDateTime.now())
                        .expiryDate(LocalDate.now().plusYears(3))
                        .build(),
                FindCardResponse.builder()
                        .cardId(cardId2)
                        .maskedNumber("****5678")
                        .balance(new BigDecimal("2500.00"))
                        .currency(Currency.USD)
                        .status(CardStatus.ACTIVE)
                        .cardType(CardType.MASTERCARD)
                        .createdAt(LocalDateTime.now())
                        .expiryDate(LocalDate.now().plusYears(4))
                        .build()
        );

        Page<FindCardResponse> cardPage = new PageImpl<>(cards, PageRequest.of(page, size), 2);

        when(userCardService.findUserCards(any(FindAllCardRequest.class)))
                .thenReturn(cardPage);

        mockMvc.perform(get("/api/v1/user/cards")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].cardId").value(cardId1.toString()))
                .andExpect(jsonPath("$.content[0].maskedNumber").value("****1234"))
                .andExpect(jsonPath("$.content[0].balance").value(1000.50))
                .andExpect(jsonPath("$.content[0].currency").value("USD"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].cardType").value("MIR"))
                .andExpect(jsonPath("$.content[1].cardId").value(cardId2.toString()))
                .andExpect(jsonPath("$.content[1].maskedNumber").value("****5678"))
                .andExpect(jsonPath("$.content[1].balance").value(2500.00))
                .andExpect(jsonPath("$.content[1].currency").value("USD"))
                .andExpect(jsonPath("$.content[1].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[1].cardType").value("MASTERCARD"))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getUserCards_WhenPageIsNegative_ThenReturnBadRequest() throws Exception {
        int page = -1;
        int size = 10;

        mockMvc.perform(get("/api/v1/user/cards")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("400 BAD REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getCardBalance_WhenCardExistsAndStatusIsValid_ThenReturnOkAndBalanceResponse() throws Exception {
        UUID cardId = UUID.randomUUID();

        BalanceResponse balanceResponse = BalanceResponse.builder()
                .maskedNumber("****1234")
                .balance(new BigDecimal("1500.75"))
                .currency(Currency.USD)
                .status(CardStatus.ACTIVE)
                .build();

        when(userCardService.findCardBalance(any(BalanceRequest.class)))
                .thenReturn(balanceResponse);

        mockMvc.perform(get("/api/v1/user/cards/{cardId}/balance", cardId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.maskedNumber").value("****1234"))
                .andExpect(jsonPath("$.balance").value(1500.75))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getCardBalance_WhenCardNotFound_ThenReturnNotFoundWithErrorResponse() throws Exception {
        UUID cardId = UUID.randomUUID();

        when(userCardService.findCardBalance(any(BalanceRequest.class)))
                .thenThrow(new NotFoundException("Card not found or does not belong to user"));

        mockMvc.perform(get("/api/v1/user/cards/{cardId}/balance", cardId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("404 NOT FOUND"))
                .andExpect(jsonPath("$.message").value("Card not found or does not belong to user"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getCardBalance_WhenCardIsNotActive_ThenReturnForbiddenWithErrorResponse() throws Exception {
        UUID cardId = UUID.randomUUID();

        when(userCardService.findCardBalance(any(BalanceRequest.class)))
                .thenThrow(new ForbiddenException("Card status is not active"));

        mockMvc.perform(get("/api/v1/user/cards/{cardId}/balance", cardId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("403 FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Card status is not active"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void transferBetweenCards_WhenRequestWithDescription_ThenReturnOk() throws Exception {
        UUID sourceCardId = UUID.randomUUID();
        UUID targetCardId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                sourceCardId,
                targetCardId,
                new BigDecimal("50.00"),
                "Payment for services"
        );

        UUID transactionId = UUID.randomUUID();
        TransferResponse response = new TransferResponse(transactionId);

        when(userCardService.transferBetweenCards(any(String.class), any(TransferRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/user/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists());
    }

    @Test
    void transferBetweenCards_WhenInsufficientFunds_ThenReturnBadRequest() throws Exception {
        UUID sourceCardId = UUID.randomUUID();
        UUID targetCardId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                sourceCardId,
                targetCardId,
                new BigDecimal("1000.00"),
                "Transfer"
        );

        when(userCardService.transferBetweenCards(anyString(), any(TransferRequest.class)))
                .thenThrow(new BadRequestException("smth"));

        mockMvc.perform(post("/api/v1/user/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400 BAD REQUEST"))
                .andExpect(jsonPath("$.message").value("smth"));
    }

    @Test
    void transferBetweenCards_WhenCardsHaveDifferentCurrencies_ThenReturnForbidden() throws Exception {
        UUID sourceCardId = UUID.randomUUID();
        UUID targetCardId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                sourceCardId,
                targetCardId,
                new BigDecimal("100.00"),
                "Transfer"
        );

        when(userCardService.transferBetweenCards(anyString(), any(TransferRequest.class)))
                .thenThrow(new ForbiddenException("Cannot transfer between different currencies"));

        mockMvc.perform(post("/api/v1/user/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("403 FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Cannot transfer between different currencies"));
    }

    @Test
    void transferBetweenCards_WhenTargetCardNotFound_ThenReturnNotFound() throws Exception {
        UUID sourceCardId = UUID.randomUUID();
        UUID targetCardId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                sourceCardId,
                targetCardId,
                new BigDecimal("100.00"),
                "Transfer"
        );

        when(userCardService.transferBetweenCards(anyString(), any(TransferRequest.class)))
                .thenThrow(new NotFoundException("Target card not found or does not belong to user"));

        mockMvc.perform(post("/api/v1/user/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("404 NOT FOUND"))
                .andExpect(jsonPath("$.message").value("Target card not found or does not belong to user"));
    }

    @Test
    void createApplication_WhenValidRequest_ThenReturnCreatedAndCreateApplicationResponse() throws Exception {
        UUID cardId = UUID.randomUUID();

        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .title("Application")
                .description(" description")
                .type(ApplicationType.DELETE_CARD)
                .build();

        UUID applicationId = UUID.randomUUID();
        CreateApplicationResponse response = new CreateApplicationResponse(applicationId);

        when(userCardService.createApplication(any(String.class), eq(cardId), any(CreateApplicationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/user/cards/{cardId}/applications", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()));
    }

    @Test
    void createApplication_WhenTitleIsBlank_ThenReturnBadRequest() throws Exception {
        UUID cardId = UUID.randomUUID();

        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .description("description")
                .type(ApplicationType.DELETE_CARD)
                .build();

        mockMvc.perform(post("/api/v1/user/cards/{cardId}/applications", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("400 BAD REQUEST"))
                .andExpect(jsonPath("$.message").exists());

    }

    @Test
    void createApplication_WhenCardNotFound_ThenReturnNotFound() throws Exception {
        UUID cardId = UUID.randomUUID();

        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .title("Application")
                .description(" description")
                .type(ApplicationType.DELETE_CARD)
                .build();

        when(userCardService.createApplication(any(String.class), eq(cardId), any(CreateApplicationRequest.class)))
                .thenThrow(new NotFoundException("Card not found or does not belong to user"));

        mockMvc.perform(post("/api/v1/user/cards/{cardId}/applications", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("404 NOT FOUND"))
                .andExpect(jsonPath("$.message").value("Card not found or does not belong to user"));
    }

    @Test
    void createApplication_WhenApplicationTypeAlreadyExists_ThenReturnConflict() throws Exception {
        UUID cardId = UUID.randomUUID();

        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .title("Application")
                .description(" description")
                .type(ApplicationType.DELETE_CARD)
                .build();

        when(userCardService.createApplication(any(String.class), eq(cardId), any(CreateApplicationRequest.class)))
                .thenThrow(new ConflictException("Application form of type DELETE_CARD already exists for this card"));

        mockMvc.perform(post("/api/v1/user/cards/{cardId}/applications", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("409 CONFLICT"))
                .andExpect(jsonPath("$.message").value("Application form of type DELETE_CARD already exists for this card"));
    }
}
