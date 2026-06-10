package com.digital.lending.payment.repository;

import com.digital.lending.payment.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface PaymentTransactionRepository extends
        JpaRepository<PaymentTransaction, String>,
        JpaSpecificationExecutor<PaymentTransaction> {

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
}
