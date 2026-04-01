package com.example.bankcards.service;

import com.example.bankcards.dto.request.BalanceRequest;
import com.example.bankcards.dto.request.CreateApplicationRequest;
import com.example.bankcards.dto.request.FindAllCardRequest;
import com.example.bankcards.dto.request.TopUpBalanceRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CreateApplicationResponse;
import com.example.bankcards.dto.response.FindCardResponse;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.ApplicationForm;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.ApplicationRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.util.CardValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCardService {

    private final ApplicationRepository applicationRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    public Page<FindCardResponse> findUserCards(FindAllCardRequest request) {
        Page<Card> cardPage = cardRepository.findAllWithFilters(request);

        return cardPage.map(card -> FindCardResponse.builder()
                .cardId(card.getId())
                .maskedNumber(card.getMaskedNumber())
                .balance(card.getBalance())
                .currency(card.getCurrency())
                .status(card.getStatus())
                .createdAt(card.getCreatedAt())
                .cardType(card.getCardType())
                .expiryDate(card.getExpiryDate())
                .build());
    }

    @Transactional(timeout = 10)
    public void topUpBalance(String email, UUID cardId, TopUpBalanceRequest request){
        Card card = cardRepository.findByIdAndUsernameWithLock(cardId, email)
                .orElseThrow(() -> new NotFoundException("Card not found or does not belong to user"));

        CardValidationUtils.validateStatus(List.of(card.getStatus()));

        card.setBalance(card.getBalance().add(request.balance()));
        cardRepository.save(card);
    }

    public BalanceResponse findCardBalance(BalanceRequest request) {
        Card card = cardRepository.findByIdAndUsername(request.cardId(), request.email())
                .orElseThrow(() -> new NotFoundException("Card not found or does not belong to user"));

        CardValidationUtils.validateStatus(List.of(card.getStatus()));

        return BalanceResponse.builder()
                .maskedNumber(card.getMaskedNumber())
                .currency(card.getCurrency())
                .balance(card.getBalance())
                .status(card.getStatus())
                .build();
    }

    @Transactional(timeout = 10)
    public TransferResponse transferBetweenCards(String email, TransferRequest request) {
        Card sourceCard = cardRepository.findByIdAndUsernameWithLock(request.sourceCardId(), email)
                .orElseThrow(() -> new NotFoundException("Source card not found or does not belong to user"));

        Card targetCard = cardRepository.findByIdAndUsernameWithLock(request.targetCardId(), email)
                .orElseThrow(() -> new NotFoundException("Target card not found or does not belong to user"));

        CardValidationUtils.validateCardsForTransfer(sourceCard, targetCard, request.amount());

        sourceCard.setBalance(sourceCard.getBalance().subtract(request.amount()));
        targetCard.setBalance(targetCard.getBalance().add(request.amount()));

        Transaction transaction = Transaction.builder()
                .amount(request.amount())
                .fromCard(sourceCard)
                .toCard(targetCard)
                .description(request.description())
                .build();

        transactionRepository.save(transaction);
        cardRepository.save(sourceCard);
        cardRepository.save(targetCard);

        return new TransferResponse(transaction.getId());
    }

    public CreateApplicationResponse createApplication(String email, UUID cardId, CreateApplicationRequest request) {
        Card card = cardRepository.findByIdAndUsername(cardId, email)
                .orElseThrow(() -> new NotFoundException("Card not found or does not belong to user"));

        card.getApplicationForms().stream()
                .filter(form -> form.getType().equals(request.type()))
                .findFirst().ifPresent(form -> {
                    throw new ConflictException("Application form of type "
                            + request.type() + " already exists for this card");
                });

        ApplicationForm form = ApplicationForm.builder()
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .card(card)
                .build();

        ApplicationForm savedForm = applicationRepository.save(form);

        return new CreateApplicationResponse(savedForm.getId());
    }
}
