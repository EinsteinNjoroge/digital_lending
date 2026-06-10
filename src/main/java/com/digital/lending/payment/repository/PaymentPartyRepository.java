package com.digital.lending.payment.repository;

import com.digital.lending.payment.model.PaymentParty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentPartyRepository extends JpaRepository<PaymentParty, String> {
    Optional<PaymentParty> findByPartyReference(String partyReference);
}
