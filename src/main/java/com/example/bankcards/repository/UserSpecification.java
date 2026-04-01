package com.example.bankcards.repository;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.UserRole;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> searchByTerm(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + search.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern)
            );
        };
    }

    public static Specification<User> hasEnabled(Boolean isEnabled) {
        return (root, query, criteriaBuilder) -> {
            if (isEnabled == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("enabled"), isEnabled);
        };
    }

    public static Specification<User> hasRole(UserRole roleName) {
        return (root, query, criteriaBuilder) -> {
            if (roleName == null) {
                return criteriaBuilder.conjunction();
            }

            Join<User, Role> roleJoin = root.join("roles", JoinType.LEFT);

            return criteriaBuilder.equal(roleJoin.get("name"), roleName);
        };
    }
}
