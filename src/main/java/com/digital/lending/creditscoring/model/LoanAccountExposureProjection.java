package com.digital.lending.creditscoring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(name = "creditscoring_loan_account_exposure_projection")
public class LoanAccountExposureProjection {

    @Id
    @Column(name = "loan_account_id", length = 50)
    private String loanAccountId;

    @Column(name = "profile_id", nullable = false, length = 50)
    private String profileId;

    @Column(name = "account_reference", length = 32)
    private String accountReference;

    @Column(name = "outstanding_principal", nullable = false, precision = 18, scale = 4)
    private BigDecimal outstandingPrincipal;

    @Column(name = "exposure_status", nullable = false, length = 32)
    private String exposureStatus;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
}
