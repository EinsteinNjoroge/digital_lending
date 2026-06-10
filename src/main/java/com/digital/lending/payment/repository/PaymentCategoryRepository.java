package com.digital.lending.payment.repository;

import com.digital.lending.payment.model.PaymentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCategoryRepository extends JpaRepository<PaymentCategory, String> {}
