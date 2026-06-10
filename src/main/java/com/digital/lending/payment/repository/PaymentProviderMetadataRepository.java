package com.digital.lending.payment.repository;

import com.digital.lending.payment.model.PaymentProviderMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentProviderMetadataRepository extends JpaRepository<PaymentProviderMetadata, String> {
    Optional<PaymentProviderMetadata> findByExternalReferenceNumber(String externalReferenceNumber);
    Optional<PaymentProviderMetadata> findByProviderTransactionId(String providerTransactionId);
    Optional<PaymentProviderMetadata> findByTransactionId(String transactionId);
}
