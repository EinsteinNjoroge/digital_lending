package com.digital.lending.payment.repository;

import com.digital.lending.payment.model.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, String> {}
