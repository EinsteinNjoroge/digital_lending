package com.digital.lending.payment.service;

import com.digital.lending.payment.model.PaymentTransaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentSpecification {

    public static Specification<PaymentTransaction> createSpecification(
            LocalDateTime fromDate, LocalDateTime toDate, String profileId,
            String accountReference, String providerId, String currency) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("initiatedAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("initiatedAt"), toDate));
            }
            if (accountReference != null && !accountReference.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("accountReference"), accountReference));
            }
            if (providerId != null && !providerId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("providerId"), providerId.toUpperCase()));
            }
            if (currency != null && !currency.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("currency"), currency.toUpperCase()));
            }
            if (profileId != null && !profileId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("profileId"), profileId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
