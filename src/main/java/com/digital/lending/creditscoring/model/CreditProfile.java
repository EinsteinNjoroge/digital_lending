package com.digital.lending.creditscoring.model;

import com.digital.lending.creditscoring.enums.CreditProfileStatus;
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
@Table(name = "credit_profile")
public class CreditProfile {

    @Id
    @Column(name = "profile_id", nullable = false, length = 64)
    private String profileId;

    @Column(name = "baseline_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal baselineScore;

    @Column(name = "introductory_credit_limit", nullable = false, precision = 18, scale = 4)
    private BigDecimal introductoryCreditLimit;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CreditProfileStatus status;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
}
