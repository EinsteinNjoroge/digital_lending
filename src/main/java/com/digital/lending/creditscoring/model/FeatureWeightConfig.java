package com.digital.lending.creditscoring.model;

import lombok.Data;

import java.util.List;

@Data
public class FeatureWeightConfig {
    private String feature;
    private List<ScoreRange> ranges;
}