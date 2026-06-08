package com.digital.lending.creditscoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoringOrchestrationEngine {

    private final RuleEvaluationEngine evaluationEngine;
    private final CreditScoringDecisionLogRepository decisionLogRepository;
    private final CreditScoringModelRepository modelRepository; // Injected to resolve database routing queries

    /**
     * Resolves active abstract rule matrix properties from database using routing coordinates
     * before running the execution evaluation engine.
     */
    public CreditDecisionResponse resolveAndEvaluate(String partnerId, String currency, ScoringRequestDto request) {
        CreditScoringModelDefinition activeModel = modelRepository.findActiveModel(partnerId, currency, request.getModelCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("No active credit scoring matrix mapped for Tenant: %s [%s] with Model: %s",
                                partnerId, currency, request.getModelCode())));

        return this.evaluateCreditRisk(
                request.getTransactionId(),
                request.getCustomerId(),
                partnerId,
                currency,
                activeModel.getId(),
                request.getFeatures(),
                activeModel.getRulesPayload()
        );
    }

    /**
     * Executes the object-mapped credit scoring risk evaluation pipeline.
     * Evaluates hard knockouts, calculates cumulative scoring parameters, computes credit bounds,
     * and persists an unalterable transaction trace snapshot for audit logs and ML profiling.
     */
    @Transactional
    public CreditDecisionResponse evaluateCreditRisk(
            String transactionId,
            String customerId,
            String partnerId,
            String currency,
            String modelDefinitionId,
            Map<String, String> resolvedFeatures,
            ScoringRulesPayload rules) { // Accepts the pre-parsed object graph directly

        Map<String, String> auditTrace = new HashMap<>();
        String decisionOutcome = "DECLINED";
        double finalScore = 0.0;
        double computedLimit = 0.0;

        try {
            // 1. Process Knock-Out (KO) Hard Filters to save computing resources
            if (rules.getKnockOutRules() != null) {
                for (KnockOutRule koRule : rules.getKnockOutRules()) {
                    boolean isTriggered = evaluationEngine.evaluatesKnockOut(koRule, resolvedFeatures);
                    if (isTriggered) {
                        decisionOutcome = "DECLINED";
                        auditTrace.put("KO_TRIGGERED", koRule.getFeature() + " met rejection condition rule boundary.");

                        return persistAndReturn(transactionId, customerId, partnerId, modelDefinitionId,
                                finalScore, decisionOutcome, computedLimit, resolvedFeatures, auditTrace);
                    }
                }
            }

            // 2. Compute Dynamic Scoring Variables from Feature Store Context Map
            if (rules.getScoringWeights() != null) {
                for (FeatureWeightConfig weightConfig : rules.getScoringWeights()) {
                    double pointsAllocated = evaluationEngine.calculateFeaturePoints(weightConfig, resolvedFeatures);
                    finalScore += pointsAllocated;
                    auditTrace.put("POINTS_" + weightConfig.getFeature(), String.valueOf(pointsAllocated));
                }
            }

            // 3. Threshold Evaluation Matrix Verification
            double minRequired = rules.getDecisionThresholds() != null ? rules.getDecisionThresholds().getMinimumScoreRequired() : 0.0;
            if (finalScore < minRequired) {
                decisionOutcome = "DECLINED";
                auditTrace.put("DECISION_REASON", "Calculated total score " + finalScore + " fell below baseline metric rule target " + minRequired);

                return persistAndReturn(transactionId, customerId, partnerId, modelDefinitionId,
                        finalScore, decisionOutcome, computedLimit, resolvedFeatures, auditTrace);
            }

            // 4. Dynamic Limit Matrix Custom Calibration
            // Resolves baseline transactional volume reference variable directly from features bucket payload
            double referenceVolume = Double.parseDouble(resolvedFeatures.getOrDefault("wallet_throughput_30d", "0.0"));
            double baseMultiplier = rules.getDecisionThresholds() != null ? rules.getDecisionThresholds().getBaseMultiplier() : 0.0;
            computedLimit = referenceVolume * baseMultiplier;

            decisionOutcome = "APPROVED";
            auditTrace.put("DECISION_REASON", "Scorecard profile criteria passed operational boundaries completely.");

        } catch (Exception e) {
            log.error("Failed to execute risk matrix evaluation loop context for customer: {}", customerId, e);
            decisionOutcome = "REFERRED"; // Safe fallback configuration target for runtime exceptions
            auditTrace.put("ENGINE_SYSTEM_ERROR", e.getMessage());
            computedLimit = 0.0;
            finalScore = 0.0;
        }

        return persistAndReturn(transactionId, customerId, partnerId, modelDefinitionId,
                finalScore, decisionOutcome, computedLimit, resolvedFeatures, auditTrace);
    }

    /**
     * Builds and saves an immutable ledger log tracking the state configuration inputs/outputs
     * exactly as they existed during processing execution.
     */
    private CreditDecisionResponse persistAndReturn(
            String transactionId,
            String customerId,
            String partnerId,
            String modelDefinitionId,
            double finalScore,
            String decisionOutcome,
            double computedLimit,
            Map<String, String> resolvedFeatures,
            Map<String, String> auditTrace) {

        try {
            CreditScoringDecisionLog decisionLog = CreditScoringDecisionLog.builder()
                    .transactionId(transactionId)
                    .customerId(customerId)
                    .partnerId(partnerId)
                    .modelDefinitionId(modelDefinitionId)
                    .scoreCalculated(finalScore)
                    .decisionOutcome(decisionOutcome)
                    .creditLimitAllocated(computedLimit)
                    .featureSnapshot(resolvedFeatures)
                    .evaluationTrace(auditTrace) // Maps cleanly onto evaluationTrace property name schema
                    .evaluatedAt(ZonedDateTime.now())
                    .build();

            decisionLogRepository.save(decisionLog);

        } catch (Exception ex) {
            log.error("Critical Failure: Unable to persist structural transaction audit logs to database instance path: {}", transactionId, ex);
            // Append infrastructure errors directly onto payload response to maintain execution tracing insight visibility
            auditTrace.put("PERSISTENCE_FAULT_WARNING", "Log failed execution payload write boundary: " + ex.getMessage());
        }

        return new CreditDecisionResponse(decisionOutcome, finalScore, computedLimit, auditTrace);
    }
}