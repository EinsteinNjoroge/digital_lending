package com.digital.lending.payment.dto;

import java.time.Instant;

public record PaymentChannelResponse(
        String id,
        String name,
        Instant createdAt
) {}
