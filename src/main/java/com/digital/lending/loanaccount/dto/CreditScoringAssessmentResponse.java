package com.digital.lending.loanaccount.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Schema(name = "CreditScoringAssessmentResponse", description = "Inbound scoring response framework from underwriter appraisal systems.")
public class CreditScoringAssessmentResponse {

    @JsonProperty("id") // Maps the incoming JSON 'id' key to your internal service 'decisionId' variable context
    @Schema(example = "c2d49630-7ac0-42d7-81fb-c8b6d9a8f752", description = "Unique tracking system token reference ID")
    private String decisionId;

    @Schema(example = "APPROVED", description = "Evaluation outcomes: APPROVED, DENIED, REFER")
    private String decisionOutcome;

    @Schema(example = "6750.00", description = "Max risk exposures threshold allowed for profile")
    private BigDecimal creditLimitAllocated;

    @Schema(example = "85.0", description = "Raw scorecard point weight output configuration")
    private Double scoreCalculated;

    @Schema(description = "Map containing point tracing weights and risk matrix calculations")
    private Map<String, String> evaluationTrace;
}