package com.digital.lending.loanaccount.dto;

import com.digital.lending.loanaccount.enums.IssuanceStatus;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Schema(name = "LoanAccountResponse", description = "Complete ledger view data snapshot reflecting execution statuses.")
public class LoanAccountResponseDto {
    @Schema(example = "acc_882c4491-1c20-4e0c-bc1a-592b68f275ca")
    private String id;

    @Schema(example = "LN-2026-08123", description = "Null while processing in PENDING_SCORE_VALIDATION state. Set once an application is approved.")
    private String accountNumber;

    @Schema(example = "PROF-254700112233")
    private String profileId;

    @Schema(example = "f186e626-c8b0-4e0c-bc1a-592b68f275ca")
    private String loanProductId;

    @Schema(example = "idem_tx_saf_8812")
    private String idempotencyKey;

    @Schema(example = "5000.00")
    private BigDecimal initialPrincipal;

    @Schema(example = "2500.00")
    private BigDecimal outstandingPrincipal;

    @Schema(example = "6750", description = "The formal credit boundary cap captured from underwriter scorecard response evaluations.")
    private Integer creditLimitAtCapture;

    @Schema(example = "ACTIVE", description = "Current lifecycle phase: PENDING_SCORE_VALIDATION, APPROVED, ACTIVE, DENIED, SETTLED, CLOSED")
    private IssuanceStatus issuanceStatus;

    @Schema(example = "ACTIVE", description = "Portfolio performance classification bucket.")
    private PerformanceStatus performanceStatus;

    @Schema(example = "acc_771b3547-5c64-42ea-aee1-39930222d111")
    private String parentLoanAccountId;

    private ZonedDateTime takenAt;
    private ZonedDateTime settledAt;
}
