package com.digital.lending.loanaccount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Schema(name = "LoanAccountOpeningRequest", description = "Request to create a loan application.")
public class LoanAccountOpeningRequestDto {

    @NotBlank(message = "Profile reference identifier is required")
    @Schema(example = "PROF-254700112233", description = "Borrower profile id.")
    private String profileId;

    @NotBlank(message = "Target loan product UUID definition is required")
    @Schema(example = "f186e626-c8b0-4e0c-bc1a-592b68f275ca", description = "Loan product id.")
    private String loanProductId;

    @NotBlank(message = "Idempotency validation token is required")
    @Schema(example = "idem_tx_saf_8812")
    private String idempotencyKey;

    @NotNull(message = "Requested baseline drawdown balance is required")
    @DecimalMin(value = "0.01", message = "Initial principal must be greater than zero")
    @Schema(example = "5000.00")
    private BigDecimal initialPrincipal;

    @Schema(example = "acc_parent_9912b", description = "Optional reference code matching parent line records")
    private String parentLoanAccountId;

    @Schema(example = "SAF_KE_01", description = "Optional partner identifier used by the credit-scoring module")
    private String partnerId;

    @Schema(example = "KES", description = "Optional ISO 4217 currency code used by the credit-scoring and payment modules")
    private String currency;

    @Schema(description = "Optional features passed to the credit-scoring engine.")
    private Map<String, String> scoringFeatures;

    @Schema(example = "INTERNAL", description = "Optional provider route to use for disbursal; defaults to INTERNAL when omitted")
    private String disbursementProviderId;

    @Schema(example = "WALLET-PROF-254700112233", description = "Optional payout destination reference used by the payment module")
    private String disbursementDestinationReference;
}
