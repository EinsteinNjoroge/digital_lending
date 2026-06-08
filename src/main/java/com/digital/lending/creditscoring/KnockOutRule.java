package com.digital.lending.creditscoring;

import lombok.Data;

@Data
public class KnockOutRule {
    private String feature;
    private RuleOperator operator;
    private String value;
}