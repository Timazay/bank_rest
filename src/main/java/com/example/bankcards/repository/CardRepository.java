package com.example.bankcards.repository;

import com.example.bankcards.dto.request.FindAllCardRequest;
import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID>,
        JpaSpecificationExecutor<Card>{

    default Page<Card> findAllWithFilters(FindAllCardRequest request) {
        Specification<Card> spec = Specification
                .where(CardSpecifications.hasCardType(request.cardType()))
                .and(CardSpecifications.hasCardStatus(request.cardStatus()))
                .and(CardSpecifications.hasCurrency(request.currency()));

        Pageable pageable = PageRequest.of(request.page(), request.size());
        return findAll(spec, pageable);
    }
}
