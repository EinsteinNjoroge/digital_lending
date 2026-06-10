package com.digital.lending.loanaccount.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(name = "loan_account_product_configuration_projection")
public class LoanProductConfigurationProjection {

    @Id
    @Column(name = "loan_product_id", length = 64)
    private String loanProductId;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(name = "partner_id", nullable = false, length = 64)
    private String partnerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "repayment_due_days", nullable = false)
    private long repaymentDueDays;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
}
