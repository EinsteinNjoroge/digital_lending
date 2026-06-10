package com.digital.lending.events;

import java.time.ZonedDateTime;

public record LoanProductConfigurationChangedEvent(
        String loanProductId,
        String productCode,
        String partnerId,
        String currency,
        boolean active,
        long repaymentDueDays,
        ZonedDateTime occurredAt
) {}
