package com.digital.lending.events;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record ProviderPayoutCompletedEvent(
        String loanAccountId,
        String paymentTransactionId,
        String profileId,
        String accountReference,
        BigDecimal amount,
        String currency,
        String providerId,
        String externalReferenceNumber,
        ZonedDateTime occurredAt
) {}
