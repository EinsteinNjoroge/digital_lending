package com.digital.lending.events;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record LoanDisbursalRequestedEvent(
        String loanAccountId,
        String accountReference,
        String profileId,
        String loanProductId,
        BigDecimal amount,
        String currency,
        String providerId,
        String destinationReference,
        String actor,
        ZonedDateTime occurredAt
) {}
