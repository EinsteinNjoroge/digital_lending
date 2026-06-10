package com.digital.lending.payment.gateway;

public record PaymentGatewayInitiationResult(
        String externalReferenceNumber,
        String providerTransactionId,
        String rawPayload
) {}
