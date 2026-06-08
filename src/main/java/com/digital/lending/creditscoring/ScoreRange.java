package com.digital.lending.creditscoring;

import lombok.Data;

@Data
public class ScoreRange {
    private double min;
    private double max;
    private double points;
}