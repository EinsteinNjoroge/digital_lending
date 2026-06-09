package com.digital.lending.loanproduct;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanProductAuditLogRepository extends JpaRepository<LoanProductConfigurationAuditLog, Long> {
}
