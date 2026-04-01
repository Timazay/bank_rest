package com.example.bankcards.repository;

import com.example.bankcards.dto.request.FindAllCardRequest;
import com.example.bankcards.entity.Card;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID>,
        JpaSpecificationExecutor<Card>{

    default Page<Card> findAllWithFilters(FindAllCardRequest request) {
        Specification<Card> spec = Specification
                .where(CardSpecifications.hasCardType(request.cardType()))
                .and(CardSpecifications.hasCardStatus(request.cardStatus()))
                .and(CardSpecifications.hasCurrency(request.currency()))
                .and(CardSpecifications.hasUserEmail(request.userEmail()));

        Pageable pageable = PageRequest.of(request.page(), request.size());
        return findAll(spec, pageable);
    }

    @Query("SELECT c FROM Card c " +
            "JOIN FETCH c.user u " +
            "LEFT JOIN FETCH c.applicationForms a " +
            "WHERE c.id = :cardId AND u.email = :email")
    Optional<Card> findByIdAndUsername(@Param("cardId") UUID cardId, @Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c JOIN c.user u WHERE c.id = :cardId AND u.email = :email")
    Optional<Card> findByIdAndUsernameWithLock(@Param("cardId") UUID cardId, @Param("email") String email);
}
