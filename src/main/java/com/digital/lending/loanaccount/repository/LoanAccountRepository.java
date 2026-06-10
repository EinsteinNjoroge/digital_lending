package com.digital.lending.loanaccount.repository;

import com.digital.lending.loanaccount.enums.IssuanceStatus;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import com.digital.lending.loanaccount.model.LoanAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanAccountRepository extends JpaRepository<LoanAccount, String> {

    Optional<LoanAccount> findByIdempotencyKey(String idempotencyKey);

    Optional<LoanAccount> findByAccountNumber(String accountNumber);

    boolean existsByProfileIdAndLoanProductIdAndPerformanceStatusIn(
            String profileId,
            String loanProductId,
            Collection<PerformanceStatus> statuses
    );

    @Query("SELECT l FROM LoanAccount l WHERE " +
           "(:profileId IS NULL OR l.profileId = :profileId) AND " +
           "(:status IS NULL OR l.performanceStatus = :status)")
    Page<LoanAccount> findAllLedgerSlice(
            @Param("profileId") String profileId,
            @Param("status") PerformanceStatus status,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(l.outstandingPrincipal), 0) FROM LoanAccount l WHERE " +
           "l.profileId = :profileId AND l.id <> :currentLoanAccountId AND l.issuanceStatus IN :issuanceStatuses")
    BigDecimal sumOutstandingExposure(
            @Param("profileId") String profileId,
            @Param("currentLoanAccountId") String currentLoanAccountId,
            @Param("issuanceStatuses") Collection<IssuanceStatus> issuanceStatuses);

    @Query("SELECT l FROM LoanAccount l WHERE l.issuanceStatus IN :issuanceStatuses " +
           "AND l.performanceStatus IN :performanceStatuses " +
           "AND l.outstandingPrincipal > 0 " +
           "AND l.repaymentDueAt IS NOT NULL " +
           "AND l.repaymentDueAt <= :asOf")
    List<LoanAccount> findServicingCandidates(
            @Param("issuanceStatuses") Collection<IssuanceStatus> issuanceStatuses,
            @Param("performanceStatuses") Collection<PerformanceStatus> performanceStatuses,
            @Param("asOf") ZonedDateTime asOf);
}
