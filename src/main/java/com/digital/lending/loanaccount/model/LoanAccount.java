package com.digital.lending.loanaccount.model;

import com.digital.lending.loanaccount.enums.IssuanceStatus;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

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

    @Column(name = "outstanding_principal", nullable = false, precision = 18, scale = 4)
    private BigDecimal outstandingPrincipal;

    @Column(name = "credit_limit_at_capture")
    private Integer creditLimitAtCapture;

    @Enumerated(EnumType.STRING)
    @Column(name = "issuance_status", nullable = false, length = 32)
    private IssuanceStatus issuanceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "performance_status", length = 16)
    private PerformanceStatus performanceStatus;

    @Column(name = "parent_loan_account_id", length = 50)
    private String parentLoanAccountId;

    @Column(name = "taken_at")
    private ZonedDateTime takenAt;

    @Column(name = "settled_at")
    private ZonedDateTime settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt = ZonedDateTime.now();
}
