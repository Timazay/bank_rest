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
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardUtils cardUtils;

    public CreateCardResponse createCard(CreateCardRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("User not found with id: " + request.userId()));


        String cardNumber = cardUtils.generateCardNumber();
        String maskedNumber = cardUtils.maskCardNumber(cardNumber);

        Card card = Card.builder()
                .user(user)
                .cardNumber(cardUtils.encrypt(cardNumber))
                .maskedNumber(maskedNumber)
                .balance(BigDecimal.ZERO)
                .status(CardStatus.ACTIVE)
                .expiryDate(cardUtils.calculateExpiryDate(request.expiryMonth(), Integer.parseInt(request.expiryYear())))
                .cardType(request.cardType())
                .currency(request.currency())
                .build();

        card = cardRepository.save(card);
        return new CreateCardResponse(card.getId());
    }

    public void changeCardStatusToBlocked(UUID cardId, BlockCardRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found with id: " + cardId));
        card.setStatus(CardStatus.BLOCKED);
        card.setBlockReason(request.blockReason());
        card.setBlockedAt(LocalDateTime.now());
        cardRepository.save(card);
    }

    public void changeCardStatusToActivate(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found with id: " + cardId));
        card.setStatus(CardStatus.ACTIVE);
        card.setBlockReason(null);
        card.setBlockedAt(null);
        cardRepository.save(card);
    }

    public void changeCardStatusToDelete(UUID cardId, DeleteCardRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found with id: " + cardId));

        if (request.softDelete()) {
            card.setStatus(CardStatus.DELETED);
            cardRepository.save(card);
        } else {
            cardRepository.delete(card);
        }
    }

    public FindCardResponse findCardById(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found with id: " + cardId));

        return FindCardResponse.builder()
                .cardId(card.getId())
                .maskedNumber(card.getMaskedNumber())
                .balance(card.getBalance())
                .currency(card.getCurrency())
                .status(card.getStatus())
                .createdAt(card.getCreatedAt())
                .cardType(card.getCardType())
                .expiryDate(card.getExpiryDate())
                .build();
    }

    public Page<FindCardResponse> findAllCards(FindAllCardRequest request) {
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
}
