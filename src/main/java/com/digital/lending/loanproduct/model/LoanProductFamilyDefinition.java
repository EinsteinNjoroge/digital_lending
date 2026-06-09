package com.digital.lending.loanproduct.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "loan_products_family_definition")
public class LoanProductFamilyDefinition {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "family_code", nullable = false, unique = true, length = 32)
    private String familyCode;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "disbursement_handler_token", nullable = false, length = 64)
    private String disbursementHandlerToken;

    @Column(name = "accrual_handler_token", nullable = false, length = 64)
    private String accrualHandlerToken;

    @Column(name = "repayment_handler_token", nullable = false, length = 64)
    private String repaymentHandlerToken;

    @Column(name = "delinquency_handler_token", nullable = false, length = 64)
    private String delinquencyHandlerToken;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt = ZonedDateTime.now();
}