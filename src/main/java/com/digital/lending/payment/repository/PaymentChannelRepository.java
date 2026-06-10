package com.digital.lending.payment.repository;

import com.digital.lending.payment.model.PaymentChannel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentChannelRepository extends JpaRepository<PaymentChannel, String> {}
