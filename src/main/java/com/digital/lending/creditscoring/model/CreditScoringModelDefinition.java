package com.digital.lending.creditscoring.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.time.ZonedDateTime;

@Entity
@Table(name = "credit_scoring_model_definition")
@Data
public class CreditScoringModelDefinition {

    @Id
    private String id;

    @Column(name = "model_code", nullable = false, length = 32)
    private String loanProductId;

    @Column(name = "partner_id", nullable = false, length = 64)
    private String partnerId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Type(JsonBinaryType.class)
    @Column(name = "rules_payload", columnDefinition = "jsonb", nullable = false)
    private ScoringRulesPayload rulesPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt = ZonedDateTime.now();
}
