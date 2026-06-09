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

    @NotBlank(message = "Model code identifier is required")
    @Schema(description = "The target scorecard configuration setup token", example = "SCORECARD_NANO_KES")
    private String modelCode;

    @NotNull(message = "Rules payload configuration object graph is required")
    @Valid
    @Schema(description = "The structured domain schema rules matrix payload.")
    private ScoringRulesPayload rulesPayload;
}