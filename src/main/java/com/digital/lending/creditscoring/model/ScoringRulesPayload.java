package com.digital.lending.creditscoring.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;
import com.digital.lending.creditscoring.model.KnockOutRule;
import com.digital.lending.creditscoring.model.FeatureWeightConfig;
import com.digital.lending.creditscoring.model.DecisionThresholds;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoringRulesPayload {
    private List<KnockOutRule> knockOutRules;
    private List<FeatureWeightConfig> scoringWeights;
    private DecisionThresholds decisionThresholds;
}