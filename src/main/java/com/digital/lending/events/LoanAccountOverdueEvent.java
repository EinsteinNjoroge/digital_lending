package com.digital.lending.events;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record LoanAccountOverdueEvent(
        String loanAccountId,
        String profileId,
        String accountReference,
        BigDecimal outstandingPrincipal,
        String currency,
        int daysPastDue,
        String performanceStatus,
        ZonedDateTime occurredAt
) {}
