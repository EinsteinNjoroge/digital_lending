package com.digital.lending.payment.repository;

import com.digital.lending.payment.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionStatusRepository extends JpaRepository<TransactionStatus, String> {}
