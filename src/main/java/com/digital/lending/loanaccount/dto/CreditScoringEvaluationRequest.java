package com.digital.lending.loanaccount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@Schema(name = "CreditScoringEvaluationRequest", description = "Outbound request payload transmitted to the credit scorecard microservice engine.")
public class CreditScoringEvaluationRequest {
    @Schema(example = "tx_882c4491-1c20", description = "Unique audit execution tracing token key")
    private String transactionId;

    @Schema(example = "CUST-254700112233", description = "Target identifier lookup tracking key")
    private String customerId;

    @Schema(example = "SCORECARD_NANO_KES", description = "Algorithm configuration scorecard model identifier")
    private String modelCode;

    @Schema(description = "Alternative transaction throughput metrics mapping payload")
    private Map<String, String> features;
}
