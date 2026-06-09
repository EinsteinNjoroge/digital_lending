package com.digital.lending.loanproduct.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;
import java.util.Map;

@Data
@Entity
@Table(name = "loan_products_product_document_matrix")
public class LoanProductDocumentMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProductConfiguration product;

    @Column(name = "matrix_type", nullable = false, length = 64)
    private String matrixType;

    @Type(JsonBinaryType.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;
}