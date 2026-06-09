package com.digital.lending.loanproduct;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(
        name = "loan_products_product_configuration",
        uniqueConstraints = @UniqueConstraint(name = "uq_lp_prod_code_version_v3", columnNames = {"product_code", "version"})
)
public class LoanProductConfiguration {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "family_definition_id", nullable = false, length = 64)
    private String familyDefinitionId;

    @Column(name = "partner_id", nullable = false, length = 64)
    private String partnerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<LoanProductParameter> parameters = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<LoanProductDocumentMatrix> documentMatrices = new ArrayList<>();

    public void addParameter(String key, String value) {
        LoanProductParameter param = new LoanProductParameter();
        param.setProduct(this);
        param.setParameterKey(key);
        param.setParameterValue(value);
        this.parameters.add(param);
    }

    public void addMatrix(String matrixType, java.util.Map<String, Object> payload) {
        LoanProductDocumentMatrix matrix = new LoanProductDocumentMatrix();
        matrix.setProduct(this);
        matrix.setMatrixType(matrixType);
        matrix.setPayload(payload);
        this.documentMatrices.add(matrix);
    }
}