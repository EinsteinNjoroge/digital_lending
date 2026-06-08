package com.digital.lending.creditscoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditScoringOrchestrationEngineTest {

    @Mock
    private RuleEvaluationEngine evaluationEngine;

    @Mock
    private CreditScoringDecisionLogRepository decisionLogRepository;

    @InjectMocks
    private CreditScoringOrchestrationEngine orchestrationEngine;

    private ScoringRulesPayload sampleRules;
    private Map<String, String> resolvedFeatures;

    @BeforeEach
    void setUp() {
        resolvedFeatures = new HashMap<>();
        resolvedFeatures.put("sim_age_days", "120");
        resolvedFeatures.put("wallet_throughput_30d", "50000.00");

        DecisionThresholds thresholds = new DecisionThresholds();
        thresholds.setMinimumScoreRequired(50.0);
        thresholds.setBaseMultiplier(0.10);

        FeatureWeightConfig weightConfig = new FeatureWeightConfig();
        weightConfig.setFeature("wallet_throughput_30d");

        sampleRules = new ScoringRulesPayload();
        sampleRules.setDecisionThresholds(thresholds);
        sampleRules.setScoringWeights(Collections.singletonList(weightConfig));
        sampleRules.setKnockOutRules(Collections.emptyList());
    }

    @Test
    @DisplayName("Should fully approve credit risk matrix when profile clears all thresholds completely")
    void shouldReturnApprovedWhenCriteriaPassesSuccessfully() {
        String txId = "tx-approved-123";

        when(evaluationEngine.calculateFeaturePoints(any(FeatureWeightConfig.class), eq(resolvedFeatures)))
                .thenReturn(65.0);

        CreditDecisionResponse response = orchestrationEngine.evaluateCreditRisk(
                txId, "CUST-001", "SAF_KE_01", "KES", "MODEL-V1", resolvedFeatures, sampleRules);

        assertThat(response).isNotNull();
        assertThat(response.getDecisionOutcome()).isEqualTo("APPROVED");
        assertThat(response.getScoreCalculated()).isEqualTo(65.0);
        assertThat(response.getCreditLimitAllocated()).isEqualTo(5000.00);
        assertThat(response.getEvaluationTrace()).containsKey("DECISION_REASON");

        verify(decisionLogRepository, times(1)).save(any(CreditScoringDecisionLog.class));
    }

    @Test
    @DisplayName("Should short-circuit pipeline and decline profile instantly when knockout filter triggers")
    void shouldDeclineImmediatelyWhenKnockOutRuleTriggers() {
        String txId = "tx-knockout-123";
        KnockOutRule koRule = new KnockOutRule();
        koRule.setFeature("sim_age_days");
        sampleRules.setKnockOutRules(Collections.singletonList(koRule));

        when(evaluationEngine.evaluatesKnockOut(eq(koRule), eq(resolvedFeatures))).thenReturn(true);

        CreditDecisionResponse response = orchestrationEngine.evaluateCreditRisk(
                txId, "CUST-002", "SAF_KE_01", "KES", "MODEL-V1", resolvedFeatures, sampleRules);

        assertThat(response).isNotNull();
        assertThat(response.getDecisionOutcome()).isEqualTo("DECLINED");
        assertThat(response.getScoreCalculated()).isEqualTo(0.0);
        assertThat(response.getCreditLimitAllocated()).isEqualTo(0.0);
        assertThat(response.getEvaluationTrace()).containsKey("KO_TRIGGERED");

        verify(evaluationEngine, never()).calculateFeaturePoints(any(), any());
        verify(decisionLogRepository, times(1)).save(any(CreditScoringDecisionLog.class));
    }

    @Test
    @DisplayName("Should decline configuration layout profile if cumulative points fall below baseline thresholds")
    void shouldDeclineWhenFinalScoreIsBelowMinimumRequiredThreshold() {
        String txId = "tx-lowscore-123";

        when(evaluationEngine.calculateFeaturePoints(any(FeatureWeightConfig.class), eq(resolvedFeatures)))
                .thenReturn(30.0);

        CreditDecisionResponse response = orchestrationEngine.evaluateCreditRisk(
                txId, "CUST-003", "SAF_KE_01", "KES", "MODEL-V1", resolvedFeatures, sampleRules);

        assertThat(response).isNotNull();
        assertThat(response.getDecisionOutcome()).isEqualTo("DECLINED");
        assertThat(response.getScoreCalculated()).isEqualTo(30.0);
        assertThat(response.getCreditLimitAllocated()).isEqualTo(0.0);
        assertThat(response.getEvaluationTrace().get("DECISION_REASON")).contains("fell below baseline metric");

        verify(decisionLogRepository, times(1)).save(any(CreditScoringDecisionLog.class));
    }

    @Test
    @DisplayName("Should route customer to REFERRED fallback status when runtime exceptions occur")
    void shouldHandleRuntimeExceptionsGracefullyAndReferProfile() {
        String txId = "tx-exception-123";

        when(evaluationEngine.calculateFeaturePoints(any(FeatureWeightConfig.class), eq(resolvedFeatures)))
                .thenThrow(new RuntimeException("Telemetry connection lost"));

        CreditDecisionResponse response = orchestrationEngine.evaluateCreditRisk(
                txId, "CUST-004", "SAF_KE_01", "KES", "MODEL-V1", resolvedFeatures, sampleRules);

        assertThat(response).isNotNull();
        assertThat(response.getDecisionOutcome()).isEqualTo("REFERRED");
        assertThat(response.getScoreCalculated()).isEqualTo(0.0);
        assertThat(response.getCreditLimitAllocated()).isEqualTo(0.0);
        assertThat(response.getEvaluationTrace()).containsKey("ENGINE_SYSTEM_ERROR");
        assertThat(response.getEvaluationTrace().get("ENGINE_SYSTEM_ERROR")).isEqualTo("Telemetry connection lost");

        verify(decisionLogRepository, times(1)).save(any(CreditScoringDecisionLog.class));
    }

    @Test
    @DisplayName("Should continue processing when ledger repository save fails to guard application execution pipeline")
    void shouldMaintainExecutionTraceVisibilityWhenPersistenceFaultWarningTriggers() {
        String txId = "tx-db-fault-123";
        when(evaluationEngine.calculateFeaturePoints(any(FeatureWeightConfig.class), eq(resolvedFeatures)))
                .thenReturn(75.0);

        doThrow(new RuntimeException("Database write deadlock exception"))
                .when(decisionLogRepository).save(any(CreditScoringDecisionLog.class));

        CreditDecisionResponse response = orchestrationEngine.evaluateCreditRisk(
                txId, "CUST-005", "SAF_KE_01", "KES", "MODEL-V1", resolvedFeatures, sampleRules);

        assertThat(response).isNotNull();
        assertThat(response.getDecisionOutcome()).isEqualTo("APPROVED");
        assertThat(response.getEvaluationTrace()).containsKey("PERSISTENCE_FAULT_WARNING");
        assertThat(response.getEvaluationTrace().get("PERSISTENCE_FAULT_WARNING")).contains("Database write deadlock");
    }
}