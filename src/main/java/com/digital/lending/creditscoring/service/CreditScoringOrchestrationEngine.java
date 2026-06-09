package com.digital.lending.creditscoring.service;

import com.digital.lending.creditscoring.dto.CreditDecisionResponse;
import com.digital.lending.creditscoring.dto.ScoringRequestDto;
import com.digital.lending.creditscoring.model.CreditScoringDecisionLog;
import com.digital.lending.creditscoring.model.CreditScoringModelDefinition;
import com.digital.lending.creditscoring.model.FeatureWeightConfig;
import com.digital.lending.creditscoring.model.KnockOutRule;
import com.digital.lending.creditscoring.model.ScoringRulesPayload;
import com.digital.lending.creditscoring.repository.CreditScoringDecisionLogRepository;
import com.digital.lending.creditscoring.repository.CreditScoringModelRepository;
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
    private final CreditScoringModelRepository modelRepository;

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

    @Transactional
    public CreditDecisionResponse evaluateCreditRisk(
            String transactionId,
            String customerId,
            String partnerId,
            String currency,
            String modelDefinitionId,
            Map<String, String> resolvedFeatures,
            ScoringRulesPayload rules) {

        Map<String, String> auditTrace = new HashMap<>();
        String decisionOutcome = "DECLINED";
        double finalScore = 0.0;
        double computedLimit = 0.0;

        try {
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

            if (rules.getScoringWeights() != null) {
                for (FeatureWeightConfig weightConfig : rules.getScoringWeights()) {
                    double pointsAllocated = evaluationEngine.calculateFeaturePoints(weightConfig, resolvedFeatures);
                    finalScore += pointsAllocated;
                    auditTrace.put("POINTS_" + weightConfig.getFeature(), String.valueOf(pointsAllocated));
                }
            }

            double minRequired = rules.getDecisionThresholds() != null ? rules.getDecisionThresholds().getMinimumScoreRequired() : 0.0;
            if (finalScore < minRequired) {
                decisionOutcome = "DECLINED";
                auditTrace.put("DECISION_REASON", "Calculated total score " + finalScore + " fell below baseline metric rule target " + minRequired);

                return persistAndReturn(transactionId, customerId, partnerId, modelDefinitionId,
                        finalScore, decisionOutcome, computedLimit, resolvedFeatures, auditTrace);
            }

            double referenceVolume = Double.parseDouble(resolvedFeatures.getOrDefault("wallet_throughput_30d", "0.0"));
            double baseMultiplier = rules.getDecisionThresholds() != null ? rules.getDecisionThresholds().getBaseMultiplier() : 0.0;
            computedLimit = referenceVolume * baseMultiplier;

            decisionOutcome = "APPROVED";
            auditTrace.put("DECISION_REASON", "Scorecard profile criteria passed operational boundaries completely.");

        } catch (Exception e) {
            log.error("Failed to execute risk matrix evaluation loop context for customer: {}", customerId, e);
            decisionOutcome = "REFERRED";
            auditTrace.put("ENGINE_SYSTEM_ERROR", e.getMessage());
            computedLimit = 0.0;
            finalScore = 0.0;
        }

        return persistAndReturn(transactionId, customerId, partnerId, modelDefinitionId,
                finalScore, decisionOutcome, computedLimit, resolvedFeatures, auditTrace);
    }

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
                    .evaluationTrace(auditTrace)
                    .evaluatedAt(ZonedDateTime.now())
                    .build();

            decisionLogRepository.save(decisionLog);

        } catch (Exception ex) {
            log.error("Critical Failure: Unable to persist structural transaction audit logs to database instance path: {}", transactionId, ex);
            auditTrace.put("PERSISTENCE_FAULT_WARNING", "Log failed execution payload write boundary: " + ex.getMessage());
        }

        return new CreditDecisionResponse(transactionId, decisionOutcome, finalScore, computedLimit, auditTrace);
    }
}