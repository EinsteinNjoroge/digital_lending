package com.digital.lending.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEvent(
        Object source,
        String transactionId,
        String accountReference,
        String categoryId,
        String providerId,
        String statusId,
        BigDecimal amount,
        String currency,
        String externalReferenceNumber,
        LocalDateTime timestamp
) {}
