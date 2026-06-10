package com.digital.lending.loanproduct.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
@Table(
        name = "loan_products_product_parameter",
        uniqueConstraints = @UniqueConstraint(name = "uq_lp_product_param_key", columnNames = {"product_id", "parameter_key"})
)
public class LoanProductParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProductConfiguration product;

    @Column(name = "parameter_key", nullable = false, length = 64)
    private String parameterKey;

    @Column(name = "parameter_value", nullable = false, columnDefinition = "TEXT")
    private String parameterValue;
}