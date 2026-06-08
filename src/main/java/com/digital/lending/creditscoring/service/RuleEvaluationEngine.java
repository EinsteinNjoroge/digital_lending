package com.digital.lending.creditscoring.service;

import com.digital.lending.creditscoring.enums.RuleOperator;
import com.digital.lending.creditscoring.model.FeatureWeightConfig;
import com.digital.lending.creditscoring.model.KnockOutRule;
import com.digital.lending.creditscoring.model.ScoreRange;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class RuleEvaluationEngine {

    public boolean evaluatesKnockOut(KnockOutRule rule, Map<String, String> features) {
        if (!features.containsKey(rule.getFeature())) {
            return true;
        }

        String rawValue = features.get(rule.getFeature());

        switch (rule.getOperator()) {
            case EQUALS:
                return rawValue.equalsIgnoreCase(rule.getValue());
            case LESS_THAN:
                return Double.parseDouble(rawValue) < Double.parseDouble(rule.getValue());
            case GREATER_THAN:
                return Double.parseDouble(rawValue) > Double.parseDouble(rule.getValue());
            default:
                return false;
        }
    }

    public double calculateFeaturePoints(FeatureWeightConfig config, Map<String, String> features) {
        if (!features.containsKey(config.getFeature())) {
            return 0.0;
        }

        double val = Double.parseDouble(features.get(config.getFeature()));

        return config.getRanges().stream()
                .filter(range -> val >= range.getMin() && val <= range.getMax())
                .map(ScoreRange::getPoints)
                .findFirst()
                .orElse(0.0);
    }
}