package com.digital.lending.creditscoring.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoringRulesPayload {
    private List<KnockOutRule> knockOutRules;
    private List<FeatureWeightConfig> scoringWeights;
    private DecisionThresholds decisionThresholds;
}