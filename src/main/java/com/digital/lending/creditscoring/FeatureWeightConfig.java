package com.digital.lending.creditscoring;

import lombok.Data;
import java.util.List;

@Data
public class FeatureWeightConfig {
    private String feature;
    private List<ScoreRange> ranges;
}