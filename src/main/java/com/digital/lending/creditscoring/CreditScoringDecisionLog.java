package com.digital.lending.creditscoring;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import java.time.ZonedDateTime;
import java.util.Map;

@Entity
@Table(name = "credit_scoring_decision_log")
@Data
@Builder
public class CreditScoringDecisionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "partner_id", nullable = false, length = 64)
    private String partnerId;

    @Column(name = "model_definition_id", nullable = false, length = 64)
    private String modelDefinitionId;

    @Column(name = "score_calculated", nullable = false, columnDefinition = "numeric")
    private double scoreCalculated;

    @Column(name = "decision_outcome", nullable = false, length = 16)
    private String decisionOutcome;

    @Column(name = "credit_limit_allocated", nullable = false, columnDefinition = "numeric")
    private double creditLimitAllocated;

    @Type(JsonBinaryType.class)
    @Column(name = "feature_snapshot", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> featureSnapshot;

    @Type(JsonBinaryType.class)
    @Column(name = "evaluation_trace", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> evaluationTrace;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private ZonedDateTime evaluatedAt;
}