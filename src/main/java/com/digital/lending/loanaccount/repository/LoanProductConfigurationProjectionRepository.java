package com.digital.lending.loanaccount.repository;

import com.digital.lending.loanaccount.model.LoanProductConfigurationProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanProductConfigurationProjectionRepository extends JpaRepository<LoanProductConfigurationProjection, String> {
}
