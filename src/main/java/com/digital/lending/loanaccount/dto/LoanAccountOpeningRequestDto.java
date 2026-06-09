package com.digital.lending.loanaccount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "LoanAccountOpeningRequest", description = "Inbound registration payload containing exclusively localized structural reference parameters.")
public class LoanAccountOpeningRequestDto {

    @NotBlank(message = "Profile reference identifier is required")
    @Schema(example = "CUST-254700112233", description = "Unique customer tracking reference key")
    private String profileId;

    @NotBlank(message = "Target loan product UUID definition is required")
    @Schema(example = "f186e626-c8b0-4e0c-bc1a-592b68f275ca")
    private String loanProductId;

    @NotBlank(message = "Idempotency validation token is required")
    @Schema(example = "idem_tx_saf_8812")
    private String idempotencyKey;

    @NotNull(message = "Requested baseline drawdown balance is required")
    @Min(value = 1, message = "Initial principal must be greater than zero")
    @Schema(example = "5000.00")
    private BigDecimal initialPrincipal;

    @Schema(example = "acc_parent_9912b", description = "Optional reference code matching parent line records")
    private String parentLoanAccountId;
}