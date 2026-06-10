package com.digital.lending.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEvent(
        String transactionId,
        String profileId,
        String accountReference,
        String categoryId,
        String providerId,
        String statusId,
        BigDecimal amount,
        String currency,
        String externalReferenceNumber,
        LocalDateTime timestamp
) {}
