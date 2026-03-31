package com.example.bankcards.controller;

import com.example.bankcards.TestSecurityConfig;
import com.example.bankcards.dto.request.BlockCardRequest;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.DeleteCardRequest;
import com.example.bankcards.dto.request.FindAllCardRequest;
import com.example.bankcards.dto.response.CreateCardResponse;
import com.example.bankcards.dto.response.FindCardResponse;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.service.AdminCardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCardController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminCardControllerTest {

    @MockBean
    private AdminCardService adminCardService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createCard_WhenValidRequestAndAdminAuthority_ShouldReturnCreatedStatusWithCardId() throws Exception {
        CreateCardRequest request = new CreateCardRequest(UUID.randomUUID(), CardType.VISA, Currency.USD, 3, 2028);
        CreateCardResponse response = new CreateCardResponse(UUID.randomUUID());

        when(adminCardService.createCard(any(CreateCardRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardId").value(response.cardId().toString()));

        verify(adminCardService).createCard(any(CreateCardRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createCard_WhenUserNotFound_ShouldReturnNotFoundStatus() throws Exception {
        CreateCardRequest request = new CreateCardRequest(UUID.randomUUID(), CardType.VISA, Currency.USD, 3, 2028);

        when(adminCardService.createCard(any(CreateCardRequest.class)))
                .thenThrow(new NotFoundException("User not found with id: 999"));

        mockMvc.perform(post("/api/v1/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 999"));

        verify(adminCardService).createCard(any(CreateCardRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createCard_WhenInvalidRequest_ShouldReturnBadRequestStatus() throws Exception {
        CreateCardRequest request = new CreateCardRequest(null, null, null, null, null);

        mockMvc.perform(post("/api/v1/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(adminCardService, never()).createCard(any(CreateCardRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void blockCard_WhenValidRequestAndCardExists_ShouldReturnNoContentStatus() throws Exception {
        UUID cardId = UUID.randomUUID();
        BlockCardRequest request = new BlockCardRequest("Lost card");

        doNothing().when(adminCardService).changeCardStatusToBlocked(cardId, request);

        mockMvc.perform(put("/api/v1/admin/cards/{cardId}/block", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(adminCardService).changeCardStatusToBlocked(cardId, request);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void blockCard_WhenCardNotFound_ShouldReturnNotFoundStatus() throws Exception {
        UUID nonExistentCardId = UUID.randomUUID();
        BlockCardRequest request = new BlockCardRequest("Stolen card");

        doThrow(new NotFoundException("Card not found with id: " + nonExistentCardId))
                .when(adminCardService).changeCardStatusToBlocked(eq(nonExistentCardId), any(BlockCardRequest.class));

        mockMvc.perform(put("/api/v1/admin/cards/{cardId}/block", nonExistentCardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found with id: " + nonExistentCardId))
                .andExpect(jsonPath("$.status").value("404 NOT FOUND"));

        verify(adminCardService).changeCardStatusToBlocked(eq(nonExistentCardId), any(BlockCardRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void blockCard_WhenBlockReasonIsBlank_ShouldReturnBadRequestStatus() throws Exception {
        UUID cardId = UUID.randomUUID();
        BlockCardRequest request = new BlockCardRequest("");

        mockMvc.perform(put("/api/v1/admin/cards/{cardId}/block", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value("400 BAD REQUEST"));

        verify(adminCardService, never()).changeCardStatusToBlocked(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void activateCard_WhenCardExists_ShouldReturnNoContentStatus() throws Exception {
        UUID cardId = UUID.randomUUID();

        doNothing().when(adminCardService).changeCardStatusToActivate(cardId);

        mockMvc.perform(put("/api/v1/admin/cards/{cardId}/activate", cardId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(adminCardService).changeCardStatusToActivate(cardId);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void activateCard_WhenCardNotFound_ShouldReturnNotFoundStatus() throws Exception {
        UUID nonExistentCardId = UUID.randomUUID();

        doThrow(new NotFoundException("Card not found with id: " + nonExistentCardId))
                .when(adminCardService).changeCardStatusToActivate(nonExistentCardId);

        mockMvc.perform(put("/api/v1/admin/cards/{cardId}/activate", nonExistentCardId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found with id: " + nonExistentCardId))
                .andExpect(jsonPath("$.status").value("404 NOT FOUND"));

        verify(adminCardService).changeCardStatusToActivate(nonExistentCardId);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteCard_WhenSoftDeleteTrue_ShouldReturnNoContentStatus() throws Exception {
        UUID cardId = UUID.randomUUID();
        DeleteCardRequest request = new DeleteCardRequest(true);

        doNothing().when(adminCardService).changeCardStatusToDelete(cardId, request);

        mockMvc.perform(delete("/api/v1/admin/cards/{cardId}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(adminCardService).changeCardStatusToDelete(cardId, request);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteCard_WhenCardNotFound_ShouldReturnNotFoundStatus() throws Exception {
        UUID nonExistentCardId = UUID.randomUUID();
        DeleteCardRequest request = new DeleteCardRequest(true);

        doThrow(new NotFoundException("Card not found with id: " + nonExistentCardId))
                .when(adminCardService).changeCardStatusToDelete(eq(nonExistentCardId), any(DeleteCardRequest.class));

        mockMvc.perform(delete("/api/v1/admin/cards/{cardId}", nonExistentCardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found with id: " + nonExistentCardId))
                .andExpect(jsonPath("$.status").value("404 NOT FOUND"));

        verify(adminCardService).changeCardStatusToDelete(eq(nonExistentCardId), any(DeleteCardRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteCard_WhenSoftDeleteIsNull_ShouldReturnBadRequestStatus() throws Exception {
        UUID cardId = UUID.randomUUID();
        DeleteCardRequest request = new DeleteCardRequest(null);

        mockMvc.perform(delete("/api/v1/admin/cards/{cardId}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value("400 BAD REQUEST"));

        verify(adminCardService, never()).changeCardStatusToDelete(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void getAllCards_WhenValidPageableRequest_ShouldReturnPageOfCards() throws Exception {
        int page = 0;
        int size = 10;

        FindCardResponse card1 = FindCardResponse.builder()
                .cardId(UUID.randomUUID())
                .maskedNumber("4532****0366")
                .balance(new BigDecimal("1500.50"))
                .currency(Currency.USD)
                .status(CardStatus.ACTIVE)
                .cardType(CardType.VISA)
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        FindCardResponse card2 = FindCardResponse.builder()
                .cardId(UUID.randomUUID())
                .maskedNumber("5111****1234")
                .balance(new BigDecimal("250.00"))
                .currency(Currency.EUR)
                .status(CardStatus.BLOCKED)
                .cardType(CardType.MASTERCARD)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();

        Page<FindCardResponse> cardPage = new PageImpl<>(List.of(card1, card2));

        when(adminCardService.findAllCards(any(FindAllCardRequest.class))).thenReturn(cardPage);

        mockMvc.perform(get("/api/v1/admin/cards")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].cardId").exists())
                .andExpect(jsonPath("$.content[0].maskedNumber").value("4532****0366"))
                .andExpect(jsonPath("$.content[0].balance").value(1500.50))
                .andExpect(jsonPath("$.content[0].currency").value("USD"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[1].cardId").exists())
                .andExpect(jsonPath("$.content[1].maskedNumber").value("5111****1234"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2));

        verify(adminCardService).findAllCards(any(FindAllCardRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void getCardById_WhenCardExists_ShouldReturnCardResponse() throws Exception {
        UUID cardId = UUID.randomUUID();

        FindCardResponse response = FindCardResponse.builder()
                .cardId(cardId)
                .maskedNumber("4532****0366")
                .balance(new BigDecimal("1500.50"))
                .currency(Currency.USD)
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(5))
                .cardType(CardType.VISA)
                .build();

        when(adminCardService.findCardById(cardId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/cards/{cardId}", cardId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(cardId.toString()))
                .andExpect(jsonPath("$.maskedNumber").value("4532****0366"))
                .andExpect(jsonPath("$.balance").value(1500.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cardType").value("VISA"));

        verify(adminCardService).findCardById(cardId);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void getCardById_WhenCardNotFound_ShouldReturnNotFoundStatus() throws Exception {
        UUID nonExistentCardId = UUID.randomUUID();

        when(adminCardService.findCardById(nonExistentCardId))
                .thenThrow(new NotFoundException("Card not found with id: " + nonExistentCardId));

        mockMvc.perform(get("/api/v1/admin/cards/{cardId}", nonExistentCardId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found with id: " + nonExistentCardId))
                .andExpect(jsonPath("$.status").value("404 NOT FOUND"));

        verify(adminCardService).findCardById(nonExistentCardId);
    }
}
