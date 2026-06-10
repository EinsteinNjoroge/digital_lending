package com.digital.lending.events;

import java.time.ZonedDateTime;

public record LoanAccountSettledEvent(
        String loanAccountId,
        String profileId,
        String accountReference,
        ZonedDateTime occurredAt
) {}
