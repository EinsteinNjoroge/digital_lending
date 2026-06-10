package com.digital.lending.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentCategoryRequest(
        @NotBlank @Size(max = 50) String id,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description
) {}
