package com.digital.lending.creditscoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;
import com.digital.lending.creditscoring.model.ScoringRulesPayload;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreditScoringModelResponse", description = "The public view representation metadata of a mapped credit scoring matrix setup")
public class CreditScoringModelResponseDto {
    private String id;
    private String loanProductId;
    private String partnerId;
    private String currency;
    private boolean isActive;
    private ScoringRulesPayload rulesPayload;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
