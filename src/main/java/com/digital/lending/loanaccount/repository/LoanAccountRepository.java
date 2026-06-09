package com.digital.lending.loanaccount.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

import com.digital.lending.loanaccount.dto.LoanAccountOpeningRequestDto;
import com.digital.lending.loanaccount.dto.LoanAccountResponseDto;
import com.digital.lending.loanaccount.dto.StatusModificationRequestDto;
import com.digital.lending.loanaccount.enums.IssuanceStatus;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import com.digital.lending.loanaccount.event.DraftLoanEvent;
import com.digital.lending.loanaccount.event.LoanApprovedStatusEvent;
import com.digital.lending.loanaccount.event.LoanCreditScoreEvaluatedEvent;
import com.digital.lending.loanaccount.exception.BusinessRuleViolationException;
import com.digital.lending.loanaccount.exception.ResourceNotFoundException;
import com.digital.lending.loanaccount.model.LoanAccount;
import com.digital.lending.loanaccount.model.LoanAccountAuditLog;
import com.digital.lending.loanaccount.repository.LoanAccountAuditLogRepository;
import com.digital.lending.loanaccount.repository.LoanAccountRepository;
import com.digital.lending.loanaccount.service.LoanAccountManagementService;


@Repository
public interface LoanAccountRepository extends JpaRepository<LoanAccount, String> {

    Optional<LoanAccount> findByIdempotencyKey(String idempotencyKey);

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
}