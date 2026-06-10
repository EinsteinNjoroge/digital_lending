package com.digital.lending.loanaccount.dto;

import java.time.ZonedDateTime;

public record LoanServicingRunResponseDto(
        int accountsChecked,
        int accountsUpdated,
        int overdueAccounts,
        ZonedDateTime processedAt,
        String trigger
) {}
