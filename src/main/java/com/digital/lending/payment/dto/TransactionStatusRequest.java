package com.digital.lending.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record TransactionStatusRequest(
        @NotBlank String id,
        @NotBlank String description
) {}
