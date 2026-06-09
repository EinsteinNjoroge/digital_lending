package com.digital.lending.creditscoring.repository;


import com.digital.lending.creditscoring.model.CreditScoringDecisionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditScoringDecisionLogRepository extends JpaRepository<CreditScoringDecisionLog, Long> {
    List<CreditScoringDecisionLog> findByCustomerIdOrderByEvaluatedAtDesc(String customerId);
}