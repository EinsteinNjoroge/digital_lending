package com.digital.lending.creditscoring;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
@Schema(name = "ScoringRequest", description = "Payload structure for passing point-in-time subscriber telemetry variables to the rule matrix execution engine")
public class ScoringRequestDto {

    @NotBlank(message = "Transaction ID correlation token is required")
    @Schema(description = "Unique correlation UUID used to track this specific transactional appraisal session across downstream log systems",
            example = "c6b73a21-4f9e-4a67-b81d-723b1dcfa892")
    private String transactionId;

    @NotBlank(message = "Customer reference identification profile code is required")
    @Schema(description = "Unique primary systemic reference key identifying the evaluated customer profile",
            example = "CUST-254711223344")
    private String customerId;

    @NotBlank(message = "Model validation configuration profile matrix identifier is required")
    @Schema(description = "The target scorecard setup layout token configured for this evaluation class",
            example = "SCORECARD_NANO_KES")
    private String modelCode;

    @NotNull(message = "Resolved contextual telemetry feature payload dictionary cannot be null")
    @Schema(description = "Dynamic Map container conveying the processed subscriber feature properties resolved from the online memory store layer. Schema immune—accepts arbitrary polymorphic keys.",
            example = "{\"sim_age_days\": \"365\", \"has_active_default\": \"false\", \"wallet_throughput_30d\": \"25000.50\", \"airtime_repayment_speed_days\": \"1.5\"}")
    private Map<String, String> features;
}