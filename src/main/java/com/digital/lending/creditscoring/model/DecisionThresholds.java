package com.digital.lending.creditscoring.model;

import lombok.Data;

@Data
public class DecisionThresholds {
    private double minimumScoreRequired;
    private double baseMultiplier;
}