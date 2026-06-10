package com.digital.lending.events;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;

public record LoanApplicationCreatedEvent(
        String loanAccountId,
        String profileId,
        String loanProductId,
        BigDecimal requestedPrincipal,
        String partnerId,
        String currency,
        Map<String, String> scoringFeatures,
        String disbursementProviderId,
        String disbursementDestinationReference,
        String actor,
        ZonedDateTime occurredAt
) {}
