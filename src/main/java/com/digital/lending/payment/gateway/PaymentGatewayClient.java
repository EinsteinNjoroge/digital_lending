package com.digital.lending.payment.gateway;

public interface PaymentGatewayClient {
    PaymentGatewayInitiationResult initiatePayment(PaymentGatewayRequest request);
}
