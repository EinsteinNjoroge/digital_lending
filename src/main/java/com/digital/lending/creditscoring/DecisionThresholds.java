package com.digital.lending.creditscoring;

import lombok.Data;

@Data
public class DecisionThresholds {
    private double minimumScoreRequired;
    private double baseMultiplier;
}