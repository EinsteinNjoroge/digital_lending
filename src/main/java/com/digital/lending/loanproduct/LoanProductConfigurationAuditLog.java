package com.digital.lending.loanproduct;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Entity
@Table(name = "loan_products_product_configuration_audit_log")
public class LoanProductConfigurationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(name = "action_type", nullable = false, length = 16)
    private String actionType;

    @Column(name = "modified_by", nullable = false, length = 100)
    private String modifiedBy;

    @Type(JsonBinaryType.class)
    @Column(name = "changed_attributes", columnDefinition = "jsonb")
    private Map<String, Object> changedAttributes;

    @Column(nullable = false)
    private ZonedDateTime timestamp = ZonedDateTime.now();
}