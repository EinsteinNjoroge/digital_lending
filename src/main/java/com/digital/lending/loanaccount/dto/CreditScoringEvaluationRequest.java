package com.digital.lending.loanaccount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@Schema(name = "CreditScoringEvaluationRequest", description = "Outbound request payload transmitted to the in-process credit scorecard engine.")
public class CreditScoringEvaluationRequest {
    @Schema(example = "tx_882c4491-1c20", description = "Unique audit execution tracing token key")
    private String transactionId;

    @Schema(example = "PROF-254700112233", description = "Target profile identifier lookup key")
    private String profileId;

    @Schema(example = "LOAN_PRODUCT_NANO", description = "Loan product identifier used to resolve the active scorecard")
    private String loanProductId;

    @Schema(description = "Alternative transaction throughput metrics mapping payload")
    private Map<String, String> features;
}
