package com.digital.lending.notification.service;

import com.digital.lending.notification.model.NotificationAuditLog;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationSpecification {

    public static Specification<NotificationAuditLog> createSpecification(
            String channel, String recipient, String status, LocalDateTime fromDate, LocalDateTime toDate) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (channel != null && !channel.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("channelId"), channel.toUpperCase()));
            }
            if (recipient != null && !recipient.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("recipientDestination"), recipient));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status.toUpperCase()));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
