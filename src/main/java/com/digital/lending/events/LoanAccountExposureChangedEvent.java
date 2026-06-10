package com.digital.lending.events;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record LoanAccountExposureChangedEvent(
        String loanAccountId,
        String profileId,
        String accountReference,
        BigDecimal outstandingPrincipal,
        String exposureStatus,
        ZonedDateTime occurredAt
) {}
