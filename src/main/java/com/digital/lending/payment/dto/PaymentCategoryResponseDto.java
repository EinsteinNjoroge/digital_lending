package com.digital.lending.payment.dto;

import java.time.Instant;

public record PaymentCategoryResponseDto(
        String id,
        String name,
        String description,
        Instant createdAt
) {}
