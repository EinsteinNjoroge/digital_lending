package com.digital.lending.creditscoring.repository;

import com.digital.lending.creditscoring.model.LoanAccountExposureProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface LoanAccountExposureProjectionRepository extends JpaRepository<LoanAccountExposureProjection, String> {

    @Query("SELECT COALESCE(SUM(p.outstandingPrincipal), 0) FROM LoanAccountExposureProjection p WHERE p.profileId = :profileId AND p.loanAccountId <> :currentLoanAccountId AND p.exposureStatus IN ('APPROVED', 'ACTIVE')")
    BigDecimal sumOutstandingExposure(@Param("profileId") String profileId, @Param("currentLoanAccountId") String currentLoanAccountId);
}
