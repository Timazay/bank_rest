package com.example.bankcards.service;

import com.example.bankcards.dto.request.BlockCardRequest;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.DeleteCardRequest;
import com.example.bankcards.dto.request.FindAllCardRequest;
import com.example.bankcards.dto.response.CreateCardResponse;
import com.example.bankcards.dto.response.FindCardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminCardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardUtils cardUtils;

    @InjectMocks
    private AdminCardService adminCardService;

    @Test
    void createCard_WhenUserExistsAndValidRequest_ShouldCreateAndReturnCardId() {
        UUID userId = UUID.randomUUID();
        CreateCardRequest request = new CreateCardRequest(userId, CardType.VISA, Currency.USD);

        User user = new User();
        user.setId(userId);

        String rawCardNumber = "4532015112830366";
        String maskedNumber = "4532****0366";
        String encryptedNumber = "encrypted123";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardUtils.generateCardNumber()).thenReturn(rawCardNumber);
        when(cardUtils.maskCardNumber(rawCardNumber)).thenReturn(maskedNumber);
        when(cardUtils.encrypt(rawCardNumber)).thenReturn(encryptedNumber);
        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);

        Card savedCard = Card.builder()
                .id(UUID.randomUUID())
                .user(user)
                .cardNumber(encryptedNumber)
                .maskedNumber(maskedNumber)
                .balance(BigDecimal.ZERO)
                .status(CardStatus.ACTIVE)
                .cardType(request.cardType())
                .currency(request.currency())
                .build();

        when(cardRepository.save(cardCaptor.capture())).thenReturn(savedCard);


        CreateCardResponse response = adminCardService.createCard(request);

        assertNotNull(response);
        assertEquals(savedCard.getId(), response.cardId());

        verify(userRepository).findById(userId);
        verify(cardUtils).generateCardNumber();
        verify(cardUtils).maskCardNumber(rawCardNumber);
        verify(cardUtils).encrypt(rawCardNumber);
        verify(cardRepository).save(argThat(card ->
                card.getUser().equals(user) &&
                        card.getCardNumber().equals(encryptedNumber) &&
                        card.getMaskedNumber().equals(maskedNumber) &&
                        card.getBalance().compareTo(BigDecimal.ZERO) == 0 &&
                        card.getStatus() == CardStatus.ACTIVE &&
                        card.getCardType() == request.cardType() &&
                        card.getCurrency() == request.currency()
        ));
    }

    @Test
    void createCard_WhenUserNotFound_ShouldThrowNotFoundException() {
        UUID nonExistentUserId = UUID.randomUUID();
        CreateCardRequest request = new CreateCardRequest(nonExistentUserId, CardType.MASTERCARD, Currency.EUR);

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> adminCardService.createCard(request));

        assertEquals("User not found with id: " + nonExistentUserId, exception.getMessage());

        verify(userRepository).findById(nonExistentUserId);
        verifyNoInteractions(cardUtils);
        verifyNoInteractions(cardRepository);
    }

    @Test
    void changeCardStatusToBlocked_WhenCardExistsAndValidRequest_ShouldBlockCard() {
        UUID cardId = UUID.randomUUID();
        BlockCardRequest request = new BlockCardRequest("reason");

        Card card = Card.builder()
                .id(cardId)
                .status(CardStatus.ACTIVE)
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        adminCardService.changeCardStatusToBlocked(cardId, request);

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(cardCaptor.capture());

        Card savedCard = cardCaptor.getValue();
        assertEquals(CardStatus.BLOCKED, savedCard.getStatus());
        assertEquals(request.blockReason(), savedCard.getBlockReason());
        assertNotNull(savedCard.getBlockedAt());
        assertTrue(savedCard.getBlockedAt().isBefore(LocalDateTime.now().plusSeconds(1)));

        verify(cardRepository).findById(cardId);
    }

   @Test
    void changeCardStatusToBlocked_WhenCardNotFound_ShouldThrowNotFoundException() {
        UUID nonExistentCardId = UUID.randomUUID();
        BlockCardRequest request = new BlockCardRequest("reason");

        when(cardRepository.findById(nonExistentCardId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> adminCardService.changeCardStatusToBlocked(nonExistentCardId, request));

        assertEquals("Card not found with id: " + nonExistentCardId, exception.getMessage());

        verify(cardRepository).findById(nonExistentCardId);
    }

    @Test
    void changeCardStatusToActivate_WhenCardExists_ShouldActivateCardAndClearBlockInfo() {
        UUID cardId = UUID.randomUUID();
        LocalDateTime blockedAt = LocalDateTime.now().minusDays(1);

        Card card = Card.builder()
                .id(cardId)
                .status(CardStatus.BLOCKED)
                .blockReason("Lost card")
                .blockedAt(blockedAt)
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card cardToSave = invocation.getArgument(0);
            return cardToSave;
        });

        adminCardService.changeCardStatusToActivate(cardId);

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(cardCaptor.capture());

        Card savedCard = cardCaptor.getValue();
        assertEquals(CardStatus.ACTIVE, savedCard.getStatus());
        assertNull(savedCard.getBlockReason());
        assertNull(savedCard.getBlockedAt());

        verify(cardRepository).findById(cardId);
    }

    @Test
    void changeCardStatusToActivate_WhenCardNotFound_ShouldThrowNotFoundException() {
        UUID nonExistentCardId = UUID.randomUUID();

        when(cardRepository.findById(nonExistentCardId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> adminCardService.changeCardStatusToActivate(nonExistentCardId));

        assertEquals("Card not found with id: " + nonExistentCardId, exception.getMessage());

        verify(cardRepository).findById(nonExistentCardId);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void changeCardStatusToDelete_WhenSoftDeleteTrue_ShouldChangeStatusToDeletedAndSave() {
        UUID cardId = UUID.randomUUID();
        DeleteCardRequest request = new DeleteCardRequest(true);

        Card card = Card.builder()
                .id(cardId)
                .status(CardStatus.ACTIVE)
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminCardService.changeCardStatusToDelete(cardId, request);

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(cardCaptor.capture());
        verify(cardRepository, never()).delete(any(Card.class));

        Card savedCard = cardCaptor.getValue();
        assertEquals(CardStatus.DELETED, savedCard.getStatus());

        verify(cardRepository).findById(cardId);
    }

    @Test
    void changeCardStatusToDelete_WhenSoftDeleteFalse_ShouldDeleteCardFromDatabase() {
        UUID cardId = UUID.randomUUID();
        DeleteCardRequest request = new DeleteCardRequest(false);

        Card card = Card.builder()
                .id(cardId)
                .status(CardStatus.ACTIVE)
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        adminCardService.changeCardStatusToDelete(cardId, request);

        verify(cardRepository).delete(card);
        verify(cardRepository, never()).save(any(Card.class));
        verify(cardRepository).findById(cardId);
    }

    @Test
    void changeCardStatusToDelete_WhenCardNotFound_ShouldThrowNotFoundException() {
        UUID nonExistentCardId = UUID.randomUUID();
        DeleteCardRequest request = new DeleteCardRequest(true);

        when(cardRepository.findById(nonExistentCardId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> adminCardService.changeCardStatusToDelete(nonExistentCardId, request));

        assertEquals("Card not found with id: " + nonExistentCardId, exception.getMessage());

        verify(cardRepository).findById(nonExistentCardId);
        verify(cardRepository, never()).save(any(Card.class));
        verify(cardRepository, never()).delete(any(Card.class));
    }

    @Test
    void findCardById_WhenCardExists_ShouldReturnFindCardResponse() {
        UUID cardId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
        LocalDateTime blockedAt = LocalDateTime.now().minusDays(1);

        Card card = Card.builder()
                .id(cardId)
                .maskedNumber("4532****0366")
                .balance(new BigDecimal("1500.50"))
                .currency(Currency.USD)
                .blockedAt(blockedAt)
                .status(CardStatus.BLOCKED)
                .blockReason("Lost card")
                .createdAt(createdAt)
                .cardType(CardType.VISA)
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        FindCardResponse response = adminCardService.findCardById(cardId);

        assertNotNull(response);
        assertEquals(card.getId(), response.cardId());
        assertEquals(card.getMaskedNumber(), response.maskedNumber());
        assertEquals(card.getBalance(), response.balance());
        assertEquals(card.getCurrency(), response.currency());
        assertEquals(card.getBlockedAt(), response.blockedAt());
        assertEquals(card.getStatus(), response.status());
        assertEquals(card.getBlockReason(), response.blockReason());
        assertEquals(card.getCreatedAt(), response.createdAt());
        assertEquals(card.getCardType(), response.cardType());

        verify(cardRepository).findById(cardId);
    }

    @Test
    void findCardById_WhenCardNotFound_ShouldThrowNotFoundException() {
        UUID nonExistentCardId = UUID.randomUUID();

        when(cardRepository.findById(nonExistentCardId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> adminCardService.findCardById(nonExistentCardId));

        assertEquals("Card not found with id: " + nonExistentCardId, exception.getMessage());

        verify(cardRepository).findById(nonExistentCardId);
    }

    @Test
    void findAllCards_WhenCardsExist_ShouldReturnPageOfFindCardResponse() {
        FindAllCardRequest request =
                new FindAllCardRequest(0,10, null, null, null);

        LocalDateTime createdAt1 = LocalDateTime.now().minusDays(5);
        LocalDateTime createdAt2 = LocalDateTime.now().minusDays(3);

        Card card1 = Card.builder()
                .id(UUID.randomUUID())
                .maskedNumber("4532****0366")
                .balance(new BigDecimal("1500.50"))
                .currency(Currency.USD)
                .blockedAt(null)
                .status(CardStatus.ACTIVE)
                .blockReason(null)
                .createdAt(createdAt1)
                .cardType(CardType.VISA)
                .build();

        Card card2 = Card.builder()
                .id(UUID.randomUUID())
                .maskedNumber("5111****1234")
                .balance(new BigDecimal("250.00"))
                .currency(Currency.EUR)
                .blockedAt(LocalDateTime.now().minusDays(1))
                .status(CardStatus.BLOCKED)
                .blockReason("Lost card")
                .createdAt(createdAt2)
                .cardType(CardType.MASTERCARD)
                .build();

        Page<Card> cardPage = new PageImpl<>(List.of(card1, card2));

        when(cardRepository.findAllWithFilters(request)).thenReturn(cardPage);

        Page<FindCardResponse> responsePage = adminCardService.findAllCards(request);

        assertNotNull(responsePage);
        assertEquals(2, responsePage.getTotalElements());
        assertEquals(2, responsePage.getContent().size());

        FindCardResponse response1 = responsePage.getContent().get(0);
        assertEquals(card1.getId(), response1.cardId());
        assertEquals(card1.getMaskedNumber(), response1.maskedNumber());
        assertEquals(card1.getBalance(), response1.balance());
        assertEquals(card1.getCurrency(), response1.currency());
        assertEquals(card1.getBlockedAt(), response1.blockedAt());
        assertEquals(card1.getStatus(), response1.status());
        assertEquals(card1.getBlockReason(), response1.blockReason());
        assertEquals(card1.getCreatedAt(), response1.createdAt());
        assertEquals(card1.getCardType(), response1.cardType());

        FindCardResponse response2 = responsePage.getContent().get(1);
        assertEquals(card2.getId(), response2.cardId());
        assertEquals(card2.getMaskedNumber(), response2.maskedNumber());
        assertEquals(card2.getBalance(), response2.balance());
        assertEquals(card2.getCurrency(), response2.currency());
        assertEquals(card2.getBlockedAt(), response2.blockedAt());
        assertEquals(card2.getStatus(), response2.status());
        assertEquals(card2.getBlockReason(), response2.blockReason());
        assertEquals(card2.getCreatedAt(), response2.createdAt());
        assertEquals(card2.getCardType(), response2.cardType());

        verify(cardRepository).findAllWithFilters(request);
    }
}
