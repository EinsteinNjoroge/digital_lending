package com.digital.lending.payment.gateway;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StubPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public PaymentGatewayInitiationResult initiatePayment(PaymentGatewayRequest request) {
        String externalReference = request.providerId().toUpperCase() + "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String providerTransactionId = request.providerId().toUpperCase() + "TX" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String payload = "{\"execution\":\"ACCEPTED\",\"provider\":\"" + request.providerId() + "\",\"accountReference\":\"" + request.accountReference() + "\"}";
        return new PaymentGatewayInitiationResult(externalReference, providerTransactionId, payload);
    }
}
