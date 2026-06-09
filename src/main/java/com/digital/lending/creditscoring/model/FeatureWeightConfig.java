package com.digital.lending.creditscoring.model;

import lombok.Data;
import java.util.List;
import com.digital.lending.creditscoring.model.ScoreRange;

@Data
public class FeatureWeightConfig {
    private String feature;
    private List<ScoreRange> ranges;
}