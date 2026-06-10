package com.digital.lending.loanaccount.repository;

import com.digital.lending.loanaccount.model.LoanAccountAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface LoanAccountAuditLogRepository extends JpaRepository<LoanAccountAuditLog, String> {
    List<LoanAccountAuditLog> findByLoanAccountIdOrderByRecordedAtDesc(String loanAccountId);
}