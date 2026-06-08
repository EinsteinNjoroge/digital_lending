package com.digital.lending.creditscoring.model;

import com.digital.lending.creditscoring.enums.RuleOperator;
import lombok.Data;

@Data
public class KnockOutRule {
    private String feature;
    private RuleOperator operator;
    private String value;
}