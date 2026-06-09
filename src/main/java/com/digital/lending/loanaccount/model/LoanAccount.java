package com.digital.lending.loanaccount.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
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


@Data
@Entity
@Table(name = "loan_account_accounts")
public class LoanAccount {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "account_number", unique = true, length = 32)
    private String accountNumber;

    @Column(name = "profile_id", nullable = false, length = 50)
    private String profileId;

    @Column(name = "loan_product_id", nullable = false, length = 50)
    private String loanProductId;

    @Column(name = "idempotency_key", unique = true, nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "initial_principal", nullable = false, precision = 18, scale = 4)
    private BigDecimal initialPrincipal;

    @Column(name = "credit_limit_at_capture")
    private Integer creditLimitAtCapture;

    @Enumerated(EnumType.STRING)
    @Column(name = "issuance_status", nullable = false, length = 20)
    private IssuanceStatus issuanceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "performance_status", length = 16)
    private PerformanceStatus performanceStatus;

    @Column(name = "parent_loan_account_id", length = 50)
    private String parentLoanAccountId;

    @Column(name = "taken_at")
    private ZonedDateTime takenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt = ZonedDateTime.now();
}