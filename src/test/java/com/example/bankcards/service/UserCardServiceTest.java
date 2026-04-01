package com.example.bankcards.service;

import com.example.bankcards.dto.request.BalanceRequest;
import com.example.bankcards.dto.request.CreateApplicationRequest;
import com.example.bankcards.dto.request.FindAllCardRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CreateApplicationResponse;
import com.example.bankcards.dto.response.FindCardResponse;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.ApplicationForm;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.enums.ApplicationType;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.ApplicationRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserCardServiceTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @InjectMocks
    private UserCardService userCardService;
    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Test
    void findUserCards_WhenCardsExist_ThenReturnPageOfFindCardResponse() {
        FindAllCardRequest request = FindAllCardRequest.builder()
                .page(0)
                .size(10)
                .build();

        Card card1 = Card.builder()
                .id(UUID.randomUUID())
                .maskedNumber("****1234")
                .balance(new BigDecimal("1000.50"))
                .currency(Currency.USD)
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .cardType(CardType.MIR)
                .expiryDate(LocalDate.now().plusYears(3))
                .build();

        Card card2 = Card.builder()
                .id(UUID.randomUUID())
                .maskedNumber("****5678")
                .balance(new BigDecimal("2500.00"))
                .currency(Currency.USD)
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .cardType(CardType.VISA)
                .expiryDate(LocalDate.now().plusYears(4))
                .build();

        Page<Card> cardPage = new PageImpl<>(List.of(card1, card2),
                PageRequest.of(0, 10), 2);

        when(cardRepository.findAllWithFilters(any(FindAllCardRequest.class)))
                .thenReturn(cardPage);

        Page<FindCardResponse> result = userCardService.findUserCards(request);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);

        FindCardResponse firstResponse = result.getContent().get(0);
        assertThat(firstResponse.cardId()).isEqualTo(card1.getId());
        assertThat(firstResponse.maskedNumber()).isEqualTo("****1234");
        assertThat(firstResponse.balance()).isEqualTo(new BigDecimal("1000.50"));
        assertThat(firstResponse.currency()).isEqualTo(Currency.USD);
        assertThat(firstResponse.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(firstResponse.cardType()).isEqualTo(CardType.MIR);
        assertThat(firstResponse.createdAt()).isNotNull();
        assertThat(firstResponse.expiryDate()).isNotNull();

        FindCardResponse secondResponse = result.getContent().get(1);
        assertThat(secondResponse.cardId()).isEqualTo(card2.getId());
        assertThat(secondResponse.maskedNumber()).isEqualTo("****5678");
        assertThat(secondResponse.balance()).isEqualTo(new BigDecimal("2500.00"));
        assertThat(secondResponse.currency()).isEqualTo(Currency.USD);
        assertThat(secondResponse.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(secondResponse.cardType()).isEqualTo(CardType.VISA);
        assertThat(secondResponse.createdAt()).isNotNull();
        assertThat(secondResponse.expiryDate()).isNotNull();

        verify(cardRepository, times(1)).findAllWithFilters(request);
    }

    @Test
    void findCardBalance_WhenCardExistsAndStatusIsValid_ThenReturnBalanceResponse() {
        BalanceRequest request = new BalanceRequest("user@example.com", UUID.randomUUID());

        Card card = Card.builder()
                .id(UUID.randomUUID())
                .maskedNumber("****1234")
                .balance(new BigDecimal("1500.75"))
                .currency(Currency.USD)
                .status(CardStatus.ACTIVE)
                .build();

        when(cardRepository.findByIdAndUsername(request.cardId(), request.email()))
                .thenReturn(Optional.of(card));

        BalanceResponse result = userCardService.findCardBalance(request);

        assertThat(result).isNotNull();
        assertThat(result.maskedNumber()).isEqualTo("****1234");
        assertThat(result.balance()).isEqualTo(new BigDecimal("1500.75"));
        assertThat(result.currency()).isEqualTo(Currency.USD);
        assertThat(result.status()).isEqualTo(CardStatus.ACTIVE);

        verify(cardRepository, times(1))
                .findByIdAndUsername(request.cardId(), request.email());
        verify(cardRepository, times(1))
                .findByIdAndUsername(request.cardId(), "user@example.com");
    }

    @Test
    void findCardBalance_WhenCardNotFound_ThenThrowNotFoundException() {
        BalanceRequest request = new BalanceRequest("nonexistent@example.com", UUID.randomUUID());

        when(cardRepository.findByIdAndUsername(request.cardId(), request.email()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCardService.findCardBalance(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Card not found or does not belong to user");

        verify(cardRepository, times(1))
                .findByIdAndUsername(request.cardId(), request.email());
    }

    @Test
    void transferBetweenCards_WhenValidRequest_ThenCreateTransactionWithCorrectData() {
        String email = "user@example.com";
        UUID sourceCardId = UUID.fromString("7cfdf440-e206-4b4c-bbf1-b9c741306cce");
        UUID targetCardId = UUID.fromString("8dfef551-f317-5c5d-ccf2-c9d852417ddf");

        TransferRequest request = new TransferRequest(
                sourceCardId,
                targetCardId,
                new BigDecimal("100.50"),
                "Monthly transfer"
        );

        Card sourceCard = Card.builder()
                .id(sourceCardId)
                .maskedNumber("****1234")
                .balance(new BigDecimal("500.00"))
                .currency(Currency.USD)
                .status(CardStatus.ACTIVE)
                .build();

        Card targetCard = Card.builder()
                .id(targetCardId)
                .maskedNumber("****5678")
                .balance(new BigDecimal("200.00"))
                .currency(Currency.USD)
                .status(CardStatus.ACTIVE)
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .amount(request.amount())
                .fromCard(sourceCard)
                .toCard(targetCard)
                .description(request.description())
                .build();

        when(cardRepository.findByIdAndUsernameWithLock(request.sourceCardId(), email))
                .thenReturn(Optional.of(sourceCard));
        when(cardRepository.findByIdAndUsernameWithLock(request.targetCardId(), email))
                .thenReturn(Optional.of(targetCard));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        TransferResponse result = userCardService.transferBetweenCards(email, request);

        assertThat(result).isNotNull();

        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction capturedTransaction = transactionCaptor.getValue();

        assertThat(capturedTransaction.getAmount()).isEqualTo(new BigDecimal("100.50"));
        assertThat(capturedTransaction.getFromCard()).isEqualTo(sourceCard);
        assertThat(capturedTransaction.getToCard()).isEqualTo(targetCard);
        assertThat(capturedTransaction.getDescription()).isEqualTo("Monthly transfer");

        assertThat(sourceCard.getBalance()).isEqualTo(new BigDecimal("399.50"));
        assertThat(targetCard.getBalance()).isEqualTo(new BigDecimal("300.50"));

        verify(cardRepository, times(1)).save(sourceCard);
        verify(cardRepository, times(1)).save(targetCard);
    }

    @Test
    void transferBetweenCards_WhenSourceCardNotFound_ThenThrowNotFoundException() {
        String email = "user@example.com";
        UUID sourceCardId = UUID.fromString("7cfdf440-e206-4b4c-bbf1-b9c741306cce");
        UUID targetCardId = UUID.fromString("8dfef551-f317-5c5d-ccf2-c9d852417ddf");

        TransferRequest request = new TransferRequest(
                sourceCardId,
                targetCardId,
                new BigDecimal("100.00"),
                "Monthly transfer"
        );

        when(cardRepository.findByIdAndUsernameWithLock(request.sourceCardId(), email))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCardService.transferBetweenCards(email, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Source card not found or does not belong to user");

        verify(cardRepository, never())
                .findByIdAndUsername(any(), anyString());
        verify(transactionRepository, never())
                .save(any(Transaction.class));
        verify(cardRepository, never())
                .save(any(Card.class));
    }

    @Test
    void transferBetweenCards_WhenTargetCardNotFound_ThenThrowNotFoundException() {
        String email = "user@example.com";
        UUID sourceCardId = UUID.fromString("7cfdf440-e206-4b4c-bbf1-b9c741306cce");
        UUID targetCardId = UUID.fromString("8dfef551-f317-5c5d-ccf2-c9d852417ddf");

        TransferRequest request = new TransferRequest(
                sourceCardId,
                targetCardId,
                new BigDecimal("100.00"),
                "Monthly transfer"
        );

        Card sourceCard = Card.builder()
                .id(sourceCardId)
                .maskedNumber("****1234")
                .balance(new BigDecimal("500.00"))
                .currency(Currency.USD)
                .status(CardStatus.ACTIVE)
                .build();

        when(cardRepository.findByIdAndUsernameWithLock(request.sourceCardId(), email))
                .thenReturn(Optional.of(sourceCard));
        when(cardRepository.findByIdAndUsernameWithLock(request.targetCardId(), email))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCardService.transferBetweenCards(email, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Target card not found or does not belong to user");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
        verify(cardRepository, never())
                .save(any(Card.class));

        verify(cardRepository, times(1))
                .findByIdAndUsernameWithLock(sourceCardId, email);
        verify(cardRepository, times(1))
                .findByIdAndUsernameWithLock(targetCardId, email);
    }

    @Test
    void createApplication_WhenValidRequest_ThenReturnCreateApplicationResponse() {
        String email = "user@example.com";
        UUID cardId = UUID.randomUUID();

        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .title("Delete")
                .description("Request to delete")
                .type(ApplicationType.DELETE_CARD)
                .build();

        Card card = Card.builder()
                .id(cardId)
                .maskedNumber("****1234")
                .applicationForms(new ArrayList<>())
                .build();

        ApplicationForm savedForm = ApplicationForm.builder()
                .id(UUID.randomUUID())
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .card(card)
                .build();

        when(cardRepository.findByIdAndUsername(cardId, email)).thenReturn(Optional.of(card));
        when(applicationRepository.save(any(ApplicationForm.class))).thenReturn(savedForm);

        CreateApplicationResponse response = userCardService.createApplication(email, cardId, request);

        assertThat(response).isNotNull();
        assertThat(response.applicationId()).isEqualTo(savedForm.getId());

        verify(cardRepository, times(1)).findByIdAndUsername(cardId, email);
        verify(applicationRepository, times(1)).save(any(ApplicationForm.class));
    }

    @Test
    void createApplication_WhenCardNotFound_ThenThrowNotFoundException() {
        String email = "user@example.com";
        UUID cardId = UUID.randomUUID();

        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .title("Delete")
                .description("Request to delete")
                .type(ApplicationType.DELETE_CARD)
                .build();

        when(cardRepository.findByIdAndUsername(cardId, email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCardService.createApplication(email, cardId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Card not found or does not belong to user");

        verify(cardRepository, times(1)).findByIdAndUsername(cardId, email);
        verify(applicationRepository, never()).save(any(ApplicationForm.class));
    }

    @Test
    void createApplication_WhenApplicationTypeAlreadyExists_ThenThrowConflictException() {
        String email = "user@example.com";
        UUID cardId = UUID.randomUUID();

        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .title("Delete")
                .description("Request to delete")
                .type(ApplicationType.DELETE_CARD)
                .build();

        ApplicationForm existingForm = ApplicationForm.builder()
                .id(UUID.randomUUID())
                .title("Existing Application")
                .description("Existing description")
                .type(ApplicationType.DELETE_CARD)
                .build();

        Card card = Card.builder()
                .id(cardId)
                .maskedNumber("****1234")
                .applicationForms(new ArrayList<>())
                .build();

        card.getApplicationForms().add(existingForm);

        when(cardRepository.findByIdAndUsername(cardId, email)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> userCardService.createApplication(email, cardId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Application form of type DELETE_CARD already exists for this card");

        verify(cardRepository, times(1)).findByIdAndUsername(cardId, email);
        verify(applicationRepository, never()).save(any(ApplicationForm.class));
    }
}
