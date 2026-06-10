package com.digital.lending.events;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record LoanApplicationRejectedEvent(
        String loanAccountId,
        String decisionId,
        String profileId,
        String loanProductId,
        BigDecimal requestedPrincipal,
        String decisionOutcome,
        String rejectionReason,
        Double scoreCalculated,
        String partnerId,
        String currency,
        String actor,
        ZonedDateTime occurredAt
) {}
