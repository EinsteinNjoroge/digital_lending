package com.digital.lending.payment.gateway;

import java.math.BigDecimal;

public record PaymentGatewayRequest(
        String transactionId,
        String providerId,
        String accountReference,
        String destinationReference,
        BigDecimal amount,
        String currency
) {}
