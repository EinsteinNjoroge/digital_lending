package com.digital.lending.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentProviderRequest(
        @NotBlank String id,
        @NotBlank String channelId,
        @NotBlank String name,
        @NotBlank String isActive
) {}
