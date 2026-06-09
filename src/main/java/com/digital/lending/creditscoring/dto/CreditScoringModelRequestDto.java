package com.digital.lending.creditscoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.digital.lending.creditscoring.model.ScoringRulesPayload;

@Data
@Schema(name = "CreditScoringModelRequest", description = "Payload layout for creating or updating a credit scorecard matrix definition")
public class CreditScoringModelRequestDto {

    @NotBlank(message = "Partner ID is required")
    @Schema(description = "The unique identifier for the partner/tenant", example = "SAF_KE_01")
    private String partnerId;

    @NotBlank(message = "Currency is required")
    @Schema(description = "The currency for which this model is applicable (ISO 4217 code)", example = "KES")
    private String currency;

    @NotBlank(message = "Loan Product ID is required")
    @Schema(description = "The identifier for the loan product this model applies to", example = "LOAN_PRODUCT_NANO")
    private String loanProductId;

    @NotNull(message = "Rules payload configuration object graph is required")
    @Valid
    @Schema(description = "The structured domain schema rules matrix payload.")
    private ScoringRulesPayload rulesPayload;
}