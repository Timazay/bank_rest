package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.CardType;
import com.example.bankcards.entity.enums.Currency;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class CardSpecifications {

    public static Specification<Card> hasCardType(CardType cardType) {
        return (root, query, cb) -> cardType == null ?
                cb.conjunction() : cb.equal(root.get("cardType"), cardType);
    }

    public static Specification<Card> hasCardStatus(CardStatus cardStatus) {
        return (root, query, cb) -> cardStatus == null ?
                cb.conjunction() : cb.equal(root.get("status"), cardStatus);
    }

    public static Specification<Card> hasCurrency(Currency currency) {
        return (root, query, cb) -> currency == null ?
                cb.conjunction() : cb.equal(root.get("currency"), currency);
    }

    public static Specification<Card> hasUserEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            Join<Object, Object> userJoin = root.join("user", JoinType.INNER);

            String searchPattern = "%" + email.toLowerCase() + "%";
            return criteriaBuilder.like(
                    criteriaBuilder.lower(userJoin.get("email")),
                    searchPattern
            );
        };
    }
}
