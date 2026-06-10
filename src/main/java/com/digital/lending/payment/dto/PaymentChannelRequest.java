package com.digital.lending.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentChannelRequest(
        @NotBlank @Size(max = 50) String id,
        @NotBlank @Size(max = 100) String name
) {}
