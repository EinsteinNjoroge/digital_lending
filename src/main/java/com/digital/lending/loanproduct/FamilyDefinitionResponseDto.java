package com.digital.lending.loanproduct;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
@Schema(name = "FamilyDefinitionResponse", description = "Complete data footprint output detailing the foundational calculation engine routing map.")
public class FamilyDefinitionResponseDto {
    @Schema(example = "fdef_002")
    private String id;
    @Schema(example = "M-PESA_NANO_V1")
    private String familyCode;
    @Schema(example = "Safaricom M-Pesa Consumer Micro-Lending Family")
    private String displayName;
    @Schema(example = "mpesaB2CDisbursementHandler")
    private String disbursementHandlerToken;
    @Schema(example = "straightLineDailyAccrualHandler")
    private String accrualHandlerToken;
    @Schema(example = "fifoLedgerRepaymentSettlementHandler")
    private String repaymentHandlerToken;
    @Schema(example = "automatedCrbSoftLockDelinquencyHandler")
    private String delinquencyHandlerToken;
    @Schema(example = "true")
    private Boolean isActive;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}