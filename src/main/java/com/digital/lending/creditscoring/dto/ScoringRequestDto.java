package com.digital.lending.creditscoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@Schema(name = "ScoringRequest", description = "Payload structure for passing point-in-time profile telemetry variables to the rule matrix execution engine")
public class ScoringRequestDto {

    @NotBlank(message = "Transaction ID correlation token is required")
    @Schema(description = "Unique correlation UUID used to track this specific appraisal session across downstream log systems",
            example = "c6b73a21-4f9e-4a67-b81d-723b1dcfa892")
    private String transactionId;

    @NotBlank(message = "Profile reference identifier is required")
    @Schema(description = "Unique primary systemic reference key identifying the evaluated profile",
            example = "PROF-254711223344")
    private String profileId;

    @NotBlank(message = "Partner ID is required")
    @Schema(description = "The unique identifier for the tenant or partner requesting the evaluation",
            example = "SAF_KE_01")
    private String partnerId;

    @NotBlank(message = "Currency is required")
    @Schema(description = "ISO 4217 currency code for the evaluation context",
            example = "KES")
    private String currency;

    @NotBlank(message = "Loan product identifier is required")
    @Schema(description = "The loan product whose active scorecard should be resolved for this evaluation",
            example = "LOAN_PRODUCT_NANO")
    private String loanProductId;

    @NotNull(message = "Resolved contextual telemetry feature payload dictionary cannot be null")
    @Schema(description = "Dynamic map conveying processed profile feature properties resolved from upstream systems.",
            example = "{\"sim_age_days\": \"365\", \"has_active_default\": \"false\", \"wallet_throughput_30d\": \"25000.50\", \"airtime_repayment_speed_days\": \"1.5\"}")
    private Map<String, String> features;
}
