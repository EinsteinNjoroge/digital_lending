package com.digital.lending.events;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record LoanApplicationApprovedEvent(
        String loanAccountId,
        String decisionId,
        String profileId,
        String loanProductId,
        BigDecimal requestedPrincipal,
        BigDecimal approvedLimit,
        Double scoreCalculated,
        String partnerId,
        String currency,
        String disbursementProviderId,
        String disbursementDestinationReference,
        String actor,
        ZonedDateTime occurredAt
) {}
