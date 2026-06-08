package com.digital.lending.creditscoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreditDecisionResponse", description = "The structural output evaluation response detailing the underwriting logic boundaries resolved for the applicant scenario context")
public class CreditDecisionResponse {

    @Schema(description = "The absolute finalized operational assessment outcome state token", allowableValues = {"APPROVED", "DECLINED", "REFERRED"}, example = "APPROVED")
    private String decisionOutcome;

    @Schema(description = "The total aggregated mathematical base points evaluated across feature weight variables matrices", example = "65.50")
    private double scoreCalculated;

    @Schema(description = "The maximum allocated credit limit bounds assigned to this subscriber profile, derived via reference multiplier settings rules", example = "3750.00")
    private double creditLimitAllocated;

    @Schema(description = "Point-in-time calculation diagnostics mapping data stream. Logs rule checkpoints and weight tracking values for downstream debugging transparency.",
            example = "{\"POINTS_sim_age_days\":\"50.0\",\"POINTS_wallet_throughput_30d\":\"15.5\",\"DECISION_REASON\":\"Scorecard profile criteria passed operational boundaries completely.\"}")
    private Map<String, String> evaluationTrace;
}